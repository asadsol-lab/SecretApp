package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LendingSession
import com.example.model.WhatsAppEntry
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_audit_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_MASTER_HASH = "master_hash"
        private const val KEY_SECURITY_HINT = "security_hint"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"

        // Biometric Setting
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

        // Active Lending Session
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_ACTIVE_BORROWER = "active_borrower"
        private const val KEY_ACTIVE_START_TIME = "active_start_time"

        // Session History JSON
        private const val KEY_SESSIONS_JSON = "saved_sessions_history"

        // WhatsApp Captured Notifications JSON
        private const val KEY_WHATSAPP_LOGS_JSON = "whatsapp_notifications_log"

        // Demo data toggle (defaulted to false for real new users)
        private const val KEY_DEMO_DATA_ENABLED = "demo_data_enabled"

        // Custom App Name and Icon
        private const val KEY_CUSTOM_APP_NAME = "custom_app_name"
        private const val KEY_CUSTOM_APP_ICON = "custom_app_icon"
    }

    private fun hashPassword(password: String, salt: String = "DeviceAudit_Salt_9981"): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setupPin(masterPin: String, hint: String = ""): Boolean {
        if (masterPin.isBlank() || masterPin.length < 4) return false

        val masterHash = hashPassword(masterPin)

        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .putString(KEY_MASTER_HASH, masterHash)
            .putString(KEY_SECURITY_HINT, hint)
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIMESTAMP, 0L)
            .apply()
        return true
    }

    fun updatePin(oldPin: String, newPin: String, hint: String = ""): Boolean {
        if (!verifyPin(oldPin)) return false
        return setupPin(newPin, hint)
    }

    fun verifyPin(input: String): Boolean {
        val inputHash = hashPassword(input)
        val masterHash = prefs.getString(KEY_MASTER_HASH, "") ?: ""

        return if (inputHash == masterHash && masterHash.isNotEmpty()) {
            resetFailedAttempts()
            true
        } else {
            recordFailedAttempt()
            false
        }
    }

    private fun recordFailedAttempt() {
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val edit = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current)
        if (current >= 5) {
            edit.putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis() + 30_000L)
        }
        edit.apply()
    }

    private fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIMESTAMP, 0L)
            .apply()
    }

    fun isLockedOut(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) (lockoutUntil - now) / 1000 else 0L
    }

    fun getSecurityHint(): String {
        return prefs.getString(KEY_SECURITY_HINT, "") ?: ""
    }

    // Biometric Fingerprint
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    // Active Lending Session
    fun isSessionActive(): Boolean {
        return prefs.getLong(KEY_ACTIVE_START_TIME, 0L) > 0L
    }

    fun getActiveSession(): LendingSession? {
        val startTime = prefs.getLong(KEY_ACTIVE_START_TIME, 0L)
        if (startTime <= 0L) return null
        val id = prefs.getString(KEY_ACTIVE_SESSION_ID, UUID.randomUUID().toString()) ?: ""
        val borrower = prefs.getString(KEY_ACTIVE_BORROWER, "Guest") ?: "Guest"
        return LendingSession(
            id = id,
            borrowerName = borrower,
            startTime = startTime,
            endTime = null
        )
    }

    fun startSession(borrowerName: String): LendingSession {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val name = if (borrowerName.isBlank()) "Friend / Borrower" else borrowerName.trim()

        prefs.edit()
            .putString(KEY_ACTIVE_SESSION_ID, id)
            .putString(KEY_ACTIVE_BORROWER, name)
            .putLong(KEY_ACTIVE_START_TIME, now)
            .apply()

        return LendingSession(id = id, borrowerName = name, startTime = now, endTime = null)
    }

    fun endSession(totalCalls: Int, totalSms: Int, totalWhatsApp: Int = 0): LendingSession? {
        val current = getActiveSession() ?: return null
        val now = System.currentTimeMillis()
        val completed = current.copy(
            endTime = now,
            callsRecordedCount = totalCalls,
            smsRecordedCount = totalSms,
            whatsappRecordedCount = totalWhatsApp
        )

        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_ACTIVE_BORROWER)
            .remove(KEY_ACTIVE_START_TIME)
            .apply()

        saveSessionToHistory(completed)
        return completed
    }

    private fun saveSessionToHistory(session: LendingSession) {
        val existing = getSavedSessions().toMutableList()
        existing.add(0, session)

        val jsonArray = JSONArray()
        for (s in existing.take(50)) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("borrower", s.borrowerName)
                put("startTime", s.startTime)
                put("endTime", s.endTime ?: 0L)
                put("callsCount", s.callsRecordedCount)
                put("smsCount", s.smsRecordedCount)
                put("whatsappCount", s.whatsappRecordedCount)
                put("notes", s.notes)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SESSIONS_JSON, jsonArray.toString()).apply()
    }

    fun getSavedSessions(): List<LendingSession> {
        val raw = prefs.getString(KEY_SESSIONS_JSON, null) ?: return emptyList()
        val list = mutableListOf<LendingSession>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val endTime = obj.optLong("endTime", 0L)
                list.add(
                    LendingSession(
                        id = obj.getString("id"),
                        borrowerName = obj.getString("borrower"),
                        startTime = obj.getLong("startTime"),
                        endTime = if (endTime > 0L) endTime else null,
                        callsRecordedCount = obj.optInt("callsCount", 0),
                        smsRecordedCount = obj.optInt("smsCount", 0),
                        whatsappRecordedCount = obj.optInt("whatsappCount", 0),
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }

    // WhatsApp Notification Capture
    fun saveWhatsAppEntry(entry: WhatsAppEntry) {
        val existing = getWhatsAppLogs().toMutableList()
        if (existing.any { it.senderOrCaller == entry.senderOrCaller && it.body == entry.body && kotlin.math.abs(it.timestamp - entry.timestamp) < 3000 }) {
            return
        }
        existing.add(0, entry)

        val jsonArray = JSONArray()
        for (w in existing.take(100)) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("sender", w.senderOrCaller)
                put("body", w.body)
                put("isCall", w.isCall)
                put("timestamp", w.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_WHATSAPP_LOGS_JSON, jsonArray.toString()).apply()
    }

    fun getWhatsAppLogs(sinceTimestamp: Long = 0L): List<WhatsAppEntry> {
        val raw = prefs.getString(KEY_WHATSAPP_LOGS_JSON, null) ?: return emptyList()
        val list = mutableListOf<WhatsAppEntry>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val ts = obj.getLong("timestamp")
                if (sinceTimestamp == 0L || ts >= sinceTimestamp) {
                    list.add(
                        WhatsAppEntry(
                            id = obj.getLong("id"),
                            senderOrCaller = obj.getString("sender"),
                            body = obj.optString("body", ""),
                            isCall = obj.optBoolean("isCall", false),
                            timestamp = ts
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }

    // Clear / Delete All Local History
    fun clearAllHistory() {
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_ACTIVE_BORROWER)
            .remove(KEY_ACTIVE_START_TIME)
            .remove(KEY_SESSIONS_JSON)
            .remove(KEY_WHATSAPP_LOGS_JSON)
            .apply()
    }

    // Demo Data Toggle
    fun isDemoDataEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEMO_DATA_ENABLED, false)
    }

    fun setDemoDataEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_DATA_ENABLED, enabled).apply()
    }

    // Custom App Name and Icon
    fun getCustomAppName(): String {
        return prefs.getString(KEY_CUSTOM_APP_NAME, "Device Audit Log") ?: "Device Audit Log"
    }

    fun setCustomAppName(name: String) {
        val cleanName = if (name.isBlank()) "Device Audit Log" else name.trim()
        prefs.edit().putString(KEY_CUSTOM_APP_NAME, cleanName).apply()
    }

    fun getCustomAppIcon(): String {
        return prefs.getString(KEY_CUSTOM_APP_ICON, "SHIELD") ?: "SHIELD"
    }

    fun setCustomAppIcon(iconKey: String) {
        prefs.edit().putString(KEY_CUSTOM_APP_ICON, iconKey).apply()
    }
}

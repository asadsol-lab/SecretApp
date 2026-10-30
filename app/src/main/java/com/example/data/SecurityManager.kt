package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.LendingSession
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

enum class VerifiedPasswordType {
    MASTER_ADMIN,
    SECONDARY_BACKUP
}

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_audit_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_MASTER_HASH = "master_hash"
        private const val KEY_SECONDARY_HASH = "secondary_hash"
        private const val KEY_SECURITY_HINT = "security_hint"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"

        // Active Lending Session
        private const val KEY_ACTIVE_SESSION_ID = "active_session_id"
        private const val KEY_ACTIVE_BORROWER = "active_borrower"
        private const val KEY_ACTIVE_START_TIME = "active_start_time"

        // Session History JSON
        private const val KEY_SESSIONS_JSON = "saved_sessions_history"

        // Demo data toggle for emulator testing
        private const val KEY_DEMO_DATA_ENABLED = "demo_data_enabled"
    }

    private fun hashPassword(password: String, salt: String = "DeviceAudit_Salt_9981"): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false)
    }

    fun setupPasswords(masterPass: String, secondaryPass: String, hint: String = ""): Boolean {
        if (masterPass.isBlank() || secondaryPass.isBlank()) return false
        if (masterPass == secondaryPass) return false // Must be two distinct passwords

        val masterHash = hashPassword(masterPass)
        val secondaryHash = hashPassword(secondaryPass)

        prefs.edit()
            .putBoolean(KEY_SETUP_COMPLETED, true)
            .putString(KEY_MASTER_HASH, masterHash)
            .putString(KEY_SECONDARY_HASH, secondaryHash)
            .putString(KEY_SECURITY_HINT, hint)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_TIMESTAMP, 0L)
            .apply()
        return true
    }

    fun updatePasswords(oldAuthPass: String, newMaster: String, newSecondary: String, hint: String = ""): Boolean {
        val verified = verifyPassword(oldAuthPass)
        if (verified != VerifiedPasswordType.MASTER_ADMIN) {
            return false // Only master password can reconfigure passwords
        }
        return setupPasswords(newMaster, newSecondary, hint)
    }

    fun verifyPassword(input: String): VerifiedPasswordType? {
        val inputHash = hashPassword(input)
        val masterHash = prefs.getString(KEY_MASTER_HASH, "") ?: ""
        val secondaryHash = prefs.getString(KEY_SECONDARY_HASH, "") ?: ""

        return when {
            inputHash == masterHash && masterHash.isNotEmpty() -> {
                resetFailedAttempts()
                VerifiedPasswordType.MASTER_ADMIN
            }
            inputHash == secondaryHash && secondaryHash.isNotEmpty() -> {
                resetFailedAttempts()
                VerifiedPasswordType.SECONDARY_BACKUP
            }
            else -> {
                recordFailedAttempt()
                null
            }
        }
    }

    private fun recordFailedAttempt() {
        val current = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val edit = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, current)
        if (current >= 5) {
            // Lock out for 30 seconds after 5 failed attempts
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

    // Active Lending Session
    fun isSessionActive(): Boolean {
        return prefs.getLong(KEY_ACTIVE_START_TIME, 0L) > 0L
    }

    fun getActiveSession(): LendingSession? {
        val startTime = prefs.getLong(KEY_ACTIVE_START_TIME, 0L)
        if (startTime <= 0L) return null
        val id = prefs.getString(KEY_ACTIVE_SESSION_ID, UUID.randomUUID().toString()) ?: ""
        val borrower = prefs.getString(KEY_ACTIVE_BORROWER, "Guest / Friend") ?: "Guest"
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

    fun endSession(totalCalls: Int, totalSms: Int): LendingSession? {
        val current = getActiveSession() ?: return null
        val now = System.currentTimeMillis()
        val completed = current.copy(
            endTime = now,
            callsRecordedCount = totalCalls,
            smsRecordedCount = totalSms
        )

        // Clear active session
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_ID)
            .remove(KEY_ACTIVE_BORROWER)
            .remove(KEY_ACTIVE_START_TIME)
            .apply()

        // Save to past sessions
        saveSessionToHistory(completed)
        return completed
    }

    private fun saveSessionToHistory(session: LendingSession) {
        val existing = getSavedSessions().toMutableList()
        existing.add(0, session) // newest first

        val jsonArray = JSONArray()
        for (s in existing.take(50)) { // keep last 50
            val obj = JSONObject().apply {
                put("id", s.id)
                put("borrower", s.borrowerName)
                put("startTime", s.startTime)
                put("endTime", s.endTime ?: 0L)
                put("callsCount", s.callsRecordedCount)
                put("smsCount", s.smsRecordedCount)
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
                        notes = obj.optString("notes", "")
                    )
                )
            }
        } catch (_: Exception) {
            // fallback
        }
        return list
    }

    // Demo / Sample Data toggle
    fun isDemoDataEnabled(): Boolean {
        // Enabled by default on fresh install so user sees functional data immediately in the emulator!
        return prefs.getBoolean(KEY_DEMO_DATA_ENABLED, true)
    }

    fun setDemoDataEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEMO_DATA_ENABLED, enabled).apply()
    }
}

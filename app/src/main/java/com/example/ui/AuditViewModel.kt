package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogRepository
import com.example.data.FirestoreSyncManager
import com.example.data.SampleDataProvider
import com.example.data.SecurityManager
import com.example.data.SmsLogRepository
import com.example.model.AuditItem
import com.example.model.CallEntry
import com.example.model.LendingSession
import com.example.model.SmsEntry
import com.example.model.WhatsAppEntry
import com.example.util.DeviceOemHelper
import com.example.util.OemDeviceInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    ONBOARDING,
    SETUP,
    LOCK,
    DASHBOARD,
    ARCHIVE,
    SETTINGS
}

enum class TimeFilter(val title: String, val subtitle: String) {
    SESSION("Lending Session", "Records for this session only"),
    LAST_1_HOUR("Last 1 Hour", "Past 60 minutes"),
    LAST_4_HOURS("Last 4 Hours", "Past 4 hours"),
    TODAY("Today", "Today's complete records"),
    ALL_TIME("All Time", "All available history")
}

enum class ItemTypeFilter(val title: String) {
    ALL("All Activity"),
    CALLS_ONLY("Calls Only"),
    SMS_ONLY("SMS Only"),
    WHATSAPP_ONLY("WhatsApp")
}

data class AuditUiState(
    val currentScreen: AppScreen = AppScreen.LOCK,
    val isOnboardingCompleted: Boolean = false,
    val isSetupCompleted: Boolean = false,
    val isAuthenticated: Boolean = false,
    val lockoutSeconds: Long = 0L,
    val securityHint: String = "",
    val isBiometricEnabled: Boolean = true,
    val activeSession: LendingSession? = null,
    val savedSessions: List<LendingSession> = emptyList(),
    val calls: List<CallEntry> = emptyList(),
    val sms: List<SmsEntry> = emptyList(),
    val whatsapp: List<WhatsAppEntry> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.SESSION,
    val typeFilter: ItemTypeFilter = ItemTypeFilter.ALL,
    val searchQuery: String = "",
    val isDemoDataEnabled: Boolean = false,
    val hasCallLogPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val customAppName: String = "Device Audit Log",
    val customAppIcon: String = "SHIELD",
    val isRefreshing: Boolean = false,
    val isCloudSyncing: Boolean = false,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
    val oemDeviceInfo: OemDeviceInfo = DeviceOemHelper.getDeviceInfo(),
    val isBatteryOptimizationIgnored: Boolean = false,
    val selectedDetailItem: AuditItem? = null,
    val statusMessage: String? = null
)

class AuditViewModel(application: Application) : AndroidViewModel(application) {
    private val securityManager = SecurityManager(application)
    private val callRepo = CallLogRepository(application)
    private val smsRepo = SmsLogRepository(application)
    private val firestoreSync = FirestoreSyncManager(application)

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        checkInitialStatus()
    }

    fun checkInitialStatus() {
        val onbCompleted = securityManager.isOnboardingCompleted()
        val setupCompleted = securityManager.isSetupCompleted()
        val lockout = securityManager.isLockedOut()
        val activeSession = securityManager.getActiveSession()
        val savedSessions = securityManager.getSavedSessions()
        val isDemo = securityManager.isDemoDataEnabled()
        val hint = securityManager.getSecurityHint()
        val customName = securityManager.getCustomAppName()
        val customIcon = securityManager.getCustomAppIcon()
        val biometricEnabled = securityManager.isBiometricEnabled()
        val currentUser = firestoreSync.getCurrentUser()
        val batteryIgnored = DeviceOemHelper.isBatteryOptimizationIgnored(getApplication())

        val initialScreen = when {
            !onbCompleted -> AppScreen.ONBOARDING
            !setupCompleted -> AppScreen.SETUP
            else -> AppScreen.LOCK
        }

        _uiState.update {
            it.copy(
                isOnboardingCompleted = onbCompleted,
                isSetupCompleted = setupCompleted,
                currentScreen = initialScreen,
                lockoutSeconds = lockout,
                activeSession = activeSession,
                savedSessions = savedSessions,
                isDemoDataEnabled = isDemo,
                securityHint = hint,
                customAppName = customName,
                customAppIcon = customIcon,
                isBiometricEnabled = biometricEnabled,
                userEmail = currentUser?.email,
                userDisplayName = currentUser?.displayName,
                isBatteryOptimizationIgnored = batteryIgnored,
                timeFilter = if (activeSession != null) TimeFilter.SESSION else TimeFilter.TODAY
            )
        }

        if (lockout > 0) {
            startLockoutTimer()
        }
    }

    private fun startLockoutTimer() {
        viewModelScope.launch {
            while (true) {
                val remaining = securityManager.isLockedOut()
                _uiState.update { it.copy(lockoutSeconds = remaining) }
                if (remaining <= 0) break
                delay(1000)
            }
        }
    }

    fun completeOnboarding() {
        securityManager.setOnboardingCompleted(true)
        val setupCompleted = securityManager.isSetupCompleted()
        _uiState.update {
            it.copy(
                isOnboardingCompleted = true,
                currentScreen = if (!setupCompleted) AppScreen.SETUP else AppScreen.LOCK
            )
        }
    }

    fun completeSetup(pin: String, hint: String): Boolean {
        if (pin.length < 4) {
            _uiState.update { it.copy(statusMessage = "PIN must be at least 4 digits.") }
            return false
        }

        val success = securityManager.setupPin(pin, hint)
        if (success) {
            _uiState.update {
                it.copy(
                    isSetupCompleted = true,
                    isOnboardingCompleted = true,
                    isAuthenticated = true,
                    currentScreen = AppScreen.DASHBOARD,
                    statusMessage = "Master PIN successfully saved! App unlocked."
                )
            }
            refreshLogs()
            return true
        }
        return false
    }

    fun unlockWithPin(input: String): Boolean {
        if (securityManager.isLockedOut() > 0) {
            _uiState.update { it.copy(statusMessage = "Security lockout active. Please wait.") }
            return false
        }

        val verified = securityManager.verifyPin(input)
        if (verified) {
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    currentScreen = AppScreen.DASHBOARD,
                    statusMessage = null
                )
            }
            refreshLogs()
            return true
        } else {
            val remaining = securityManager.isLockedOut()
            if (remaining > 0) {
                startLockoutTimer()
            }
            _uiState.update {
                it.copy(
                    lockoutSeconds = remaining,
                    statusMessage = "Incorrect PIN! Access denied."
                )
            }
            return false
        }
    }

    fun unlockWithBiometric(): Boolean {
        if (securityManager.isLockedOut() > 0) {
            _uiState.update { it.copy(statusMessage = "Security lockout active. Please wait.") }
            return false
        }

        _uiState.update {
            it.copy(
                isAuthenticated = true,
                currentScreen = AppScreen.DASHBOARD,
                statusMessage = null
            )
        }
        refreshLogs()
        return true
    }

    fun lockApp() {
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                currentScreen = AppScreen.LOCK,
                selectedDetailItem = null
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (!_uiState.value.isAuthenticated &&
            screen != AppScreen.LOCK &&
            screen != AppScreen.SETUP &&
            screen != AppScreen.ONBOARDING
        ) {
            lockApp()
            return
        }
        _uiState.update { it.copy(currentScreen = screen, statusMessage = null) }
    }

    fun updatePermissions(callLogGranted: Boolean, smsGranted: Boolean) {
        _uiState.update {
            it.copy(
                hasCallLogPermission = callLogGranted,
                hasSmsPermission = smsGranted
            )
        }
        refreshLogs()
    }

    fun startLendingSession(borrowerName: String) {
        val newSession = securityManager.startSession(borrowerName)
        _uiState.update {
            it.copy(
                activeSession = newSession,
                timeFilter = TimeFilter.SESSION,
                statusMessage = "Lending session started for '${newSession.borrowerName}'"
            )
        }
        refreshLogs()
    }

    fun endLendingSession() {
        val currentSession = _uiState.value.activeSession ?: return

        val callsCount = _uiState.value.calls.count { it.timestamp >= currentSession.startTime }
        val smsCount = _uiState.value.sms.count { it.timestamp >= currentSession.startTime }
        val waCount = _uiState.value.whatsapp.count { it.timestamp >= currentSession.startTime }

        val completed = securityManager.endSession(callsCount, smsCount, waCount)
        val saved = securityManager.getSavedSessions()

        _uiState.update {
            it.copy(
                activeSession = null,
                savedSessions = saved,
                timeFilter = TimeFilter.TODAY,
                statusMessage = "Session ended! Recorded $callsCount calls, $smsCount SMS, $waCount WhatsApp."
            )
        }
        refreshLogs()

        // Sync to Firestore if signed in
        completed?.let { session ->
            if (firestoreSync.isUserSignedIn()) {
                viewModelScope.launch {
                    firestoreSync.syncSession(session)
                }
            }
        }
    }

    fun setTimeFilter(filter: TimeFilter) {
        _uiState.update { it.copy(timeFilter = filter) }
        refreshLogs()
    }

    fun setTypeFilter(filter: ItemTypeFilter) {
        _uiState.update { it.copy(typeFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectDetailItem(item: AuditItem?) {
        _uiState.update { it.copy(selectedDetailItem = item) }
    }

    fun toggleDemoData(enabled: Boolean) {
        securityManager.setDemoDataEnabled(enabled)
        _uiState.update { it.copy(isDemoDataEnabled = enabled) }
        refreshLogs()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securityManager.setBiometricEnabled(enabled)
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun updateCustomBranding(name: String, iconKey: String) {
        securityManager.setCustomAppName(name)
        securityManager.setCustomAppIcon(iconKey)
        _uiState.update {
            it.copy(
                customAppName = securityManager.getCustomAppName(),
                customAppIcon = securityManager.getCustomAppIcon(),
                statusMessage = "App Name & Icon updated!"
            )
        }
    }

    fun changePin(oldPin: String, newPin: String, hint: String): Boolean {
        val success = securityManager.updatePin(oldPin, newPin, hint)
        if (success) {
            _uiState.update { it.copy(statusMessage = "Master PIN updated successfully!") }
            return true
        } else {
            _uiState.update { it.copy(statusMessage = "Current Master PIN was incorrect.") }
            return false
        }
    }

    // Google Sign-In & Cloud Sync
    fun onGoogleAuthSuccess() {
        val user = firestoreSync.getCurrentUser()
        _uiState.update {
            it.copy(
                userEmail = user?.email,
                userDisplayName = user?.displayName,
                statusMessage = "Signed in as ${user?.email ?: "Google User"}"
            )
        }
        syncToCloud()
    }

    fun onGoogleSignOut() {
        _uiState.update {
            it.copy(
                userEmail = null,
                userDisplayName = null,
                statusMessage = "Signed out from Google Account."
            )
        }
    }

    fun syncToCloud() {
        if (!firestoreSync.isUserSignedIn()) {
            _uiState.update { it.copy(statusMessage = "Sign in with Google to sync with Cloud Firestore.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCloudSyncing = true) }
            val ok = firestoreSync.syncAllRecords(
                sessions = _uiState.value.savedSessions,
                calls = _uiState.value.calls,
                sms = _uiState.value.sms,
                whatsapp = _uiState.value.whatsapp
            )
            _uiState.update {
                it.copy(
                    isCloudSyncing = false,
                    statusMessage = if (ok) "Cloud sync completed successfully!" else "Cloud sync failed. Check connection."
                )
            }
        }
    }

    // Clear / Delete All Audit History (Locally and on Firestore)
    fun clearAllAuditHistory() {
        viewModelScope.launch {
            securityManager.clearAllHistory()
            if (firestoreSync.isUserSignedIn()) {
                firestoreSync.deleteAllUserData()
            }
            _uiState.update {
                it.copy(
                    activeSession = null,
                    savedSessions = emptyList(),
                    calls = emptyList(),
                    sms = emptyList(),
                    whatsapp = emptyList(),
                    statusMessage = "All history permanently deleted from local storage and Cloud Firestore."
                )
            }
            refreshLogs()
        }
    }

    fun updateBatteryOptimizationState() {
        val ignored = DeviceOemHelper.isBatteryOptimizationIgnored(getApplication())
        _uiState.update { it.copy(isBatteryOptimizationIgnored = ignored) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun refreshLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val sinceTimestamp = calculateSinceTimestamp(_uiState.value.timeFilter)

            val rawCalls: List<CallEntry>
            val rawSms: List<SmsEntry>
            val rawWhatsApp: List<WhatsAppEntry> = securityManager.getWhatsAppLogs(sinceTimestamp)

            val isDemo = _uiState.value.isDemoDataEnabled
            val hasCallPerm = _uiState.value.hasCallLogPermission
            val hasSmsPerm = _uiState.value.hasSmsPermission

            if (isDemo) {
                rawCalls = SampleDataProvider.getSampleCalls(sinceTimestamp)
                rawSms = SampleDataProvider.getSampleSms(sinceTimestamp)
            } else {
                val liveCalls = if (hasCallPerm) callRepo.getCallLogs(sinceTimestamp) else emptyList()
                val liveSms = if (hasSmsPerm) smsRepo.getSmsLogs(sinceTimestamp) else emptyList()
                rawCalls = liveCalls
                rawSms = liveSms
            }

            _uiState.update {
                it.copy(
                    calls = rawCalls,
                    sms = rawSms,
                    whatsapp = rawWhatsApp,
                    isRefreshing = false
                )
            }
        }
    }

    private fun calculateSinceTimestamp(filter: TimeFilter): Long {
        val now = System.currentTimeMillis()
        return when (filter) {
            TimeFilter.SESSION -> {
                _uiState.value.activeSession?.startTime
                    ?: (now - 2 * 60 * 60 * 1000L)
            }
            TimeFilter.LAST_1_HOUR -> now - (60 * 60 * 1000L)
            TimeFilter.LAST_4_HOURS -> now - (4 * 60 * 60 * 1000L)
            TimeFilter.TODAY -> {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.timeInMillis
            }
            TimeFilter.ALL_TIME -> 0L
        }
    }
}

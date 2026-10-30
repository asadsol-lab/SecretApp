package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLogRepository
import com.example.data.SampleDataProvider
import com.example.data.SecurityManager
import com.example.data.SmsLogRepository
import com.example.data.VerifiedPasswordType
import com.example.model.AuditItem
import com.example.model.CallEntry
import com.example.model.LendingSession
import com.example.model.SmsEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    SETUP,
    LOCK,
    DASHBOARD,
    ARCHIVE,
    SETTINGS
}

enum class TimeFilter(val title: String, val subtitle: String) {
    SESSION("Lending Session", "Sirf is session ka record"),
    LAST_1_HOUR("Last 1 Hour", "Pichlay 1 ghantay ka"),
    LAST_4_HOURS("Last 4 Hours", "Pichlay 4 ghanton ka"),
    TODAY("Today", "Aaj ka mukammal record"),
    ALL_TIME("All Time", "Tamam records")
}

enum class ItemTypeFilter(val title: String) {
    ALL("All Activity"),
    CALLS_ONLY("Calls Only"),
    SMS_ONLY("SMS Only")
}

data class AuditUiState(
    val currentScreen: AppScreen = AppScreen.LOCK,
    val isSetupCompleted: Boolean = false,
    val isAuthenticated: Boolean = false,
    val authenticatedUserType: VerifiedPasswordType? = null,
    val lockoutSeconds: Long = 0L,
    val securityHint: String = "",
    val activeSession: LendingSession? = null,
    val savedSessions: List<LendingSession> = emptyList(),
    val calls: List<CallEntry> = emptyList(),
    val sms: List<SmsEntry> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.SESSION,
    val typeFilter: ItemTypeFilter = ItemTypeFilter.ALL,
    val searchQuery: String = "",
    val isDemoDataEnabled: Boolean = true,
    val hasCallLogPermission: Boolean = false,
    val hasSmsPermission: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedDetailItem: AuditItem? = null,
    val statusMessage: String? = null
)

class AuditViewModel(application: Application) : AndroidViewModel(application) {
    private val securityManager = SecurityManager(application)
    private val callRepo = CallLogRepository(application)
    private val smsRepo = SmsLogRepository(application)

    private val _uiState = MutableStateFlow(AuditUiState())
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    init {
        checkInitialStatus()
    }

    fun checkInitialStatus() {
        val setupCompleted = securityManager.isSetupCompleted()
        val lockout = securityManager.isLockedOut()
        val activeSession = securityManager.getActiveSession()
        val savedSessions = securityManager.getSavedSessions()
        val isDemo = securityManager.isDemoDataEnabled()
        val hint = securityManager.getSecurityHint()

        _uiState.update {
            it.copy(
                isSetupCompleted = setupCompleted,
                currentScreen = if (!setupCompleted) AppScreen.SETUP else AppScreen.LOCK,
                lockoutSeconds = lockout,
                activeSession = activeSession,
                savedSessions = savedSessions,
                isDemoDataEnabled = isDemo,
                securityHint = hint,
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

    fun completeSetup(masterPass: String, secondaryPass: String, hint: String): Boolean {
        if (masterPass.length < 4 || secondaryPass.length < 4) {
            _uiState.update { it.copy(statusMessage = "Passwords must be at least 4 characters long.") }
            return false
        }
        if (masterPass == secondaryPass) {
            _uiState.update { it.copy(statusMessage = "Master and Backup passwords must be different.") }
            return false
        }

        val success = securityManager.setupPasswords(masterPass, secondaryPass, hint)
        if (success) {
            _uiState.update {
                it.copy(
                    isSetupCompleted = true,
                    isAuthenticated = true,
                    authenticatedUserType = VerifiedPasswordType.MASTER_ADMIN,
                    currentScreen = AppScreen.DASHBOARD,
                    statusMessage = "Passwords successfully set! App unlocked."
                )
            }
            refreshLogs()
            return true
        }
        return false
    }

    fun unlockWithPassword(input: String): Boolean {
        if (securityManager.isLockedOut() > 0) {
            _uiState.update { it.copy(statusMessage = "Too many failed attempts. Please wait.") }
            return false
        }

        val verified = securityManager.verifyPassword(input)
        if (verified != null) {
            _uiState.update {
                it.copy(
                    isAuthenticated = true,
                    authenticatedUserType = verified,
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
                    statusMessage = "Incorrect password! Access denied."
                )
            }
            return false
        }
    }

    fun lockApp() {
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                authenticatedUserType = null,
                currentScreen = AppScreen.LOCK,
                selectedDetailItem = null
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (!_uiState.value.isAuthenticated && screen != AppScreen.LOCK && screen != AppScreen.SETUP) {
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

        // Compute items during this session
        val callsCount = _uiState.value.calls.count { it.timestamp >= currentSession.startTime }
        val smsCount = _uiState.value.sms.count { it.timestamp >= currentSession.startTime }

        val completed = securityManager.endSession(callsCount, smsCount)
        val saved = securityManager.getSavedSessions()

        _uiState.update {
            it.copy(
                activeSession = null,
                savedSessions = saved,
                timeFilter = TimeFilter.TODAY,
                statusMessage = "Session ended! Recorded $callsCount calls and $smsCount SMS messages."
            )
        }
        refreshLogs()
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

    fun changePasswords(oldMaster: String, newMaster: String, newSecondary: String, hint: String): Boolean {
        val success = securityManager.updatePasswords(oldMaster, newMaster, newSecondary, hint)
        if (success) {
            _uiState.update { it.copy(statusMessage = "Passwords updated successfully!") }
            return true
        } else {
            _uiState.update { it.copy(statusMessage = "Current Master Password was incorrect.") }
            return false
        }
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

            val isDemo = _uiState.value.isDemoDataEnabled
            val hasCallPerm = _uiState.value.hasCallLogPermission
            val hasSmsPerm = _uiState.value.hasSmsPermission

            if (isDemo || (!hasCallPerm && !hasSmsPerm)) {
                // If demo mode or permissions not yet granted, fetch rich sample data
                rawCalls = SampleDataProvider.getSampleCalls(sinceTimestamp)
                rawSms = SampleDataProvider.getSampleSms(sinceTimestamp)
            } else {
                // Fetch from device content resolver
                val liveCalls = if (hasCallPerm) callRepo.getCallLogs(sinceTimestamp) else emptyList()
                val liveSms = if (hasSmsPerm) smsRepo.getSmsLogs(sinceTimestamp) else emptyList()

                // If device logs are empty (e.g. fresh phone/emulator), fallback gracefully to demo
                if (liveCalls.isEmpty() && liveSms.isEmpty() && isDemo) {
                    rawCalls = SampleDataProvider.getSampleCalls(sinceTimestamp)
                    rawSms = SampleDataProvider.getSampleSms(sinceTimestamp)
                } else {
                    rawCalls = liveCalls
                    rawSms = liveSms
                }
            }

            _uiState.update {
                it.copy(
                    calls = rawCalls,
                    sms = rawSms,
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
                    ?: (now - 2 * 60 * 60 * 1000L) // fallback to 2 hrs if no active session
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

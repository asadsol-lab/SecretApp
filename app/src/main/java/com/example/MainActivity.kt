package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.Telephony
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.AuditViewModel
import com.example.ui.screens.ArchiveScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SetupPasswordScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Navy900
import com.example.util.BiometricHelper

class MainActivity : FragmentActivity() {

    private var auditViewModel: AuditViewModel? = null
    private var callLogObserver: ContentObserver? = null
    private var smsLogObserver: ContentObserver? = null

    private val auditUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            auditViewModel?.refreshLogs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val filter = IntentFilter("com.example.AUDIT_DATA_CHANGED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(auditUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(auditUpdateReceiver, filter)
            }
        } catch (_: Exception) {}

        // Register ContentObservers for real-time Call and SMS updates
        try {
            val mainHandler = Handler(Looper.getMainLooper())
            callLogObserver = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    auditViewModel?.refreshLogs()
                }
            }
            contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, callLogObserver!!)

            smsLogObserver = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    auditViewModel?.refreshLogs()
                }
            }
            contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsLogObserver!!)
        } catch (_: Exception) {}

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy900
                ) {
                    val viewModel: AuditViewModel = viewModel()
                    auditViewModel = viewModel
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    // Permission launcher for Call Log, SMS, and Contacts
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val callGranted = permissions[Manifest.permission.READ_CALL_LOG] == true
                        val smsGranted = permissions[Manifest.permission.READ_SMS] == true
                        viewModel.updatePermissions(callGranted, smsGranted)
                    }

                    // Check initial permissions and auto-prompt when entering dashboard
                    LaunchedEffect(uiState.currentScreen) {
                        if (uiState.currentScreen == AppScreen.DASHBOARD) {
                            viewModel.checkPermissionsAndRefresh()
                            if (!uiState.hasCallLogPermission || !uiState.hasSmsPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALL_LOG,
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.READ_CONTACTS
                                    )
                                )
                            }
                        }
                    }

                    // Handle back button for sub-screens and auto-lock security
                    BackHandler(enabled = true) {
                        when (uiState.currentScreen) {
                            AppScreen.ARCHIVE -> viewModel.navigateTo(AppScreen.DASHBOARD)
                            AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.DASHBOARD)
                            AppScreen.DASHBOARD -> {
                                if (uiState.isAutoLockOnBack) {
                                    viewModel.lockApp()
                                }
                                finish()
                            }
                            AppScreen.LOCK, AppScreen.SETUP, AppScreen.ONBOARDING -> finish()
                        }
                    }

                    when (uiState.currentScreen) {
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(
                                oemInfo = uiState.oemDeviceInfo,
                                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                                onComplete = { viewModel.completeOnboarding() }
                            )
                        }

                        AppScreen.SETUP -> {
                            SetupPasswordScreen(
                                onSetupComplete = { masterPin, hint ->
                                    viewModel.completeSetup(masterPin, hint)
                                },
                                errorMessage = uiState.statusMessage
                            )
                        }

                        AppScreen.LOCK -> {
                            LockScreen(
                                customAppName = uiState.customAppName,
                                isBiometricEnabled = uiState.isBiometricEnabled,
                                onUnlockWithPin = { pin ->
                                    viewModel.unlockWithPin(pin)
                                },
                                onBiometricClick = {
                                    promptBiometricUnlock(
                                        appName = uiState.customAppName,
                                        onSuccess = { viewModel.unlockWithBiometric() }
                                    )
                                },
                                lockoutSeconds = uiState.lockoutSeconds,
                                securityHint = uiState.securityHint,
                                errorMessage = uiState.statusMessage
                            )
                        }

                        AppScreen.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                onStartSession = { borrower ->
                                    viewModel.startLendingSession(borrower)
                                },
                                onEndSession = {
                                    viewModel.endLendingSession()
                                },
                                onTimeFilterSelected = { filter ->
                                    viewModel.setTimeFilter(filter)
                                },
                                onTypeFilterSelected = { filter ->
                                    viewModel.setTypeFilter(filter)
                                },
                                onSearchChanged = { query ->
                                    viewModel.setSearchQuery(query)
                                },
                                onItemClick = { item ->
                                    viewModel.selectDetailItem(item)
                                },
                                onDismissDetail = {
                                    viewModel.selectDetailItem(null)
                                },
                                onRefresh = {
                                    viewModel.refreshLogs()
                                },
                                onLockApp = {
                                    viewModel.lockApp()
                                },
                                onOpenArchive = {
                                    viewModel.navigateTo(AppScreen.ARCHIVE)
                                },
                                onOpenSettings = {
                                    viewModel.navigateTo(AppScreen.SETTINGS)
                                },
                                onRequestPermissions = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALL_LOG,
                                            Manifest.permission.READ_SMS,
                                            Manifest.permission.READ_CONTACTS
                                        )
                                    )
                                },
                                onLoadFullHistory = { viewModel.loadFullHistory() }
                            )
                        }

                        AppScreen.ARCHIVE -> {
                            ArchiveScreen(
                                savedSessions = uiState.savedSessions,
                                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                            )
                        }

                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                customAppName = uiState.customAppName,
                                customAppIcon = uiState.customAppIcon,
                                onUpdateBranding = { name, iconKey ->
                                    viewModel.updateCustomBranding(name, iconKey)
                                },
                                userEmail = uiState.userEmail,
                                userDisplayName = uiState.userDisplayName,
                                isCloudSyncing = uiState.isCloudSyncing,
                                onGoogleAuthSuccess = { viewModel.onGoogleAuthSuccess() },
                                onGoogleSignOut = { viewModel.onGoogleSignOut() },
                                onSyncNow = { viewModel.syncToCloud() },
                                isBiometricEnabled = uiState.isBiometricEnabled,
                                onToggleBiometric = { enabled -> viewModel.setBiometricEnabled(enabled) },
                                isAutoLockOnBack = uiState.isAutoLockOnBack,
                                onToggleAutoLock = { enabled -> viewModel.setAutoLockOnBack(enabled) },
                                isDemoDataEnabled = uiState.isDemoDataEnabled,
                                onToggleDemoData = { enabled -> viewModel.toggleDemoData(enabled) },
                                onChangePin = { oldPin, newPin, hint ->
                                    viewModel.changePin(oldPin, newPin, hint)
                                },
                                onDeleteAllHistory = { viewModel.clearAllAuditHistory() },
                                oemInfo = uiState.oemDeviceInfo,
                                isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
                                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun promptBiometricUnlock(
        appName: String,
        onSuccess: () -> Unit
    ) {
        if (!BiometricHelper.isBiometricAvailable(this)) {
            return
        }

        BiometricHelper.authenticateWithBiometrics(
            activity = this,
            title = "Unlock $appName",
            subtitle = "Scan your fingerprint or face to access audit logs",
            onSuccess = onSuccess,
            onError = { /* fallback to PIN */ },
            onFailed = { /* invalid biometric, prompt continues or fallbacks */ }
        )
    }

    override fun onResume() {
        super.onResume()
        if (auditViewModel?.uiState?.value?.currentScreen == AppScreen.DASHBOARD) {
            auditViewModel?.checkPermissionsAndRefresh()
        }
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock when user leaves the app or switches to home screen if enabled
        if (auditViewModel?.uiState?.value?.isAutoLockOnBack == true) {
            auditViewModel?.lockApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(auditUpdateReceiver)
        } catch (_: Exception) {}
        try {
            callLogObserver?.let { contentResolver.unregisterContentObserver(it) }
            smsLogObserver?.let { contentResolver.unregisterContentObserver(it) }
        } catch (_: Exception) {}
    }
}

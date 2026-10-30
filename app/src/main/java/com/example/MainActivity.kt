package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Navy900
                ) {
                    val viewModel: AuditViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    // Permission launcher for Call Log and SMS
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val callGranted = permissions[Manifest.permission.READ_CALL_LOG] == true
                        val smsGranted = permissions[Manifest.permission.READ_SMS] == true
                        viewModel.updatePermissions(callGranted, smsGranted)
                    }

                    // Check initial permissions
                    LaunchedEffect(Unit) {
                        val callOk = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_CALL_LOG
                        ) == PackageManager.PERMISSION_GRANTED

                        val smsOk = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED

                        viewModel.updatePermissions(callOk, smsOk)
                    }

                    // Handle back button for sub-screens and auto-lock security
                    BackHandler(enabled = true) {
                        when (uiState.currentScreen) {
                            AppScreen.ARCHIVE -> viewModel.navigateTo(AppScreen.DASHBOARD)
                            AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.DASHBOARD)
                            AppScreen.DASHBOARD -> viewModel.lockApp()
                            AppScreen.LOCK, AppScreen.SETUP, AppScreen.ONBOARDING -> finish()
                        }
                    }

                    when (uiState.currentScreen) {
                        AppScreen.ONBOARDING -> {
                            OnboardingScreen(
                                onComplete = { viewModel.completeOnboarding() }
                            )
                        }

                        AppScreen.SETUP -> {
                            SetupPasswordScreen(
                                onSetupComplete = { master, secondary, hint ->
                                    viewModel.completeSetup(master, secondary, hint)
                                },
                                errorMessage = uiState.statusMessage
                            )
                        }

                        AppScreen.LOCK -> {
                            LockScreen(
                                customAppName = uiState.customAppName,
                                onUnlock = { password ->
                                    viewModel.unlockWithPassword(password)
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
                                }
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
                                isDemoDataEnabled = uiState.isDemoDataEnabled,
                                onToggleDemoData = { enabled ->
                                    viewModel.toggleDemoData(enabled)
                                },
                                onChangePasswords = { oldMaster, newMaster, newSecondary, hint ->
                                    viewModel.changePasswords(oldMaster, newMaster, newSecondary, hint)
                                },
                                onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) }
                            )
                        }
                    }
                }
            }
        }
    }
}

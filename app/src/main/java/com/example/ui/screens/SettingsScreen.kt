package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.ui.theme.CardDark
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.util.DeviceOemHelper
import com.example.util.OemDeviceInfo
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class IconOption(
    val key: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    customAppName: String,
    customAppIcon: String,
    onUpdateBranding: (name: String, iconKey: String) -> Unit,
    userEmail: String?,
    userDisplayName: String?,
    isCloudSyncing: Boolean,
    onGoogleAuthSuccess: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    isBiometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    isDemoDataEnabled: Boolean,
    onToggleDemoData: (Boolean) -> Unit,
    onChangePin: (oldPin: String, newPin: String, hint: String) -> Boolean,
    onDeleteAllHistory: () -> Unit,
    oemInfo: OemDeviceInfo,
    isBatteryOptimizationIgnored: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isChangingPin by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var newHint by remember { mutableStateOf("") }
    var formMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isSigningInGoogle by remember { mutableStateOf(false) }

    // Custom branding state
    var appNameInput by remember { mutableStateOf(customAppName) }
    var selectedIconKey by remember { mutableStateOf(customAppIcon) }
    var brandingMessage by remember { mutableStateOf<String?>(null) }

    val iconOptions = remember {
        listOf(
            IconOption("SHIELD", "Security", Icons.Default.Security),
            IconOption("CALCULATOR", "Calculator", Icons.Default.Calculate),
            IconOption("NOTES", "Notes", Icons.Default.Description),
            IconOption("SETTINGS", "Settings", Icons.Default.Settings),
            IconOption("VAULT", "Vault", Icons.Default.Lock)
        )
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Cloud Backup",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Slate200)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy800)
            )
        },
        containerColor = Navy900
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Google Account & Cloud Firestore Sync
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Cyan400.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Google Account & Cloud Sync",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (userEmail != null) {
                        // User Signed In
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Navy800, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userDisplayName ?: "Google User",
                                    color = Slate200,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = userEmail,
                                    color = Slate400,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                enabled = !isCloudSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync to Cloud", fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val auth = FirebaseAuth.getInstance()
                                    auth.signOut()
                                    coroutineScope.launch {
                                        try {
                                            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                                        } catch (_: Exception) {}
                                        onGoogleSignOut()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign Out", color = Slate400)
                            }
                        }
                    } else {
                        // User Not Signed In
                        Text(
                            text = "Sign in with your Google account to automatically back up all call, SMS, and WhatsApp lending logs to your secure Cloud Firestore database for free.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 18.sp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                isSigningInGoogle = true
                                val credentialManager = CredentialManager.create(context)
                                val clientId = "374231705551-9negdlo9e4f5h68o4vva4hj3ec9umvk3.apps.googleusercontent.com"
                                val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                                val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                                coroutineScope.launch {
                                    try {
                                        val result = credentialManager.getCredential(context as Activity, request)
                                        val cred = result.credential
                                        if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdToken = GoogleIdTokenCredential.createFrom(cred.data).idToken
                                            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                                            FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                                            onGoogleAuthSuccess()
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        // Dismissed
                                    } catch (_: Exception) {
                                        // Fallback
                                    } finally {
                                        isSigningInGoogle = false
                                    }
                                }
                            },
                            enabled = !isSigningInGoogle,
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningInGoogle) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign in with Google", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 2: Biometric Fingerprint & Security
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Authentication & Biometrics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Emerald400, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Fingerprint Biometric Unlock", color = Slate200, style = MaterialTheme.typography.titleSmall)
                                Text("Use registered fingerprint to unseal", color = Slate400, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = onToggleBiometric,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Navy900,
                                checkedTrackColor = Emerald400,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Navy700
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isChangingPin) {
                        Button(
                            onClick = { isChangingPin = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Slate200),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Master PIN")
                        }
                    } else {
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = { currentPin = it },
                            label = { Text("Current Master PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = Slate200,
                                unfocusedTextColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("New Master PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = Slate200,
                                unfocusedTextColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newHint,
                            onValueChange = { newHint = it },
                            label = { Text("New PIN Hint (Optional)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = Slate200,
                                unfocusedTextColor = Slate200
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (formMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = formMessage ?: "",
                                color = if (isSuccess) Emerald400 else Rose500,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (newPin.length < 4) {
                                        formMessage = "PIN must be at least 4 digits."
                                        isSuccess = false
                                        return@Button
                                    }
                                    val ok = onChangePin(currentPin, newPin, newHint)
                                    if (ok) {
                                        isSuccess = true
                                        formMessage = "Master PIN updated successfully!"
                                        currentPin = ""
                                        newPin = ""
                                    } else {
                                        isSuccess = false
                                        formMessage = "Current Master PIN was incorrect."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save PIN", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isChangingPin = false
                                    formMessage = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Slate200),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Section 3: Device OEM Optimization
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Emerald400.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OEM Device Optimization",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                )
                            )
                            Text(
                                text = "${oemInfo.brandDisplayName} (${oemInfo.model})",
                                color = Emerald400,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = oemInfo.guidanceDescription,
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 18.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { DeviceOemHelper.openBatteryOptimizationSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Slate200),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBatteryOptimizationIgnored) "Battery Optimization Disabled (Active)" else "Open Battery Settings & Disable Restriction")
                    }
                }
            }

            // Section 4: WhatsApp Notification Access
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WhatsApp Audit Access",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Android notification listener service allows privacy-compliant logging of incoming WhatsApp messages and voice/video call notifications.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 18.sp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366), contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Notification Listener Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 5: App Disguise & Custom Name/Icon
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Disguise & Custom Icon",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the top bar branding title and disguise icon to suit your preference.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        label = { Text("Display App Name") },
                        placeholder = { Text("e.g. Calculator, Notes, Device Audit") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan400,
                            unfocusedBorderColor = Navy700,
                            focusedTextColor = Slate200,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { opt ->
                            val isSelected = selectedIconKey == opt.key
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) Cyan400.copy(alpha = 0.2f) else Navy800,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Cyan400 else Navy700,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedIconKey = opt.key }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = opt.icon,
                                    contentDescription = opt.label,
                                    tint = if (isSelected) Cyan400 else Slate400,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = opt.label,
                                    color = if (isSelected) Cyan400 else Slate400,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    if (brandingMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = brandingMessage ?: "",
                            color = Emerald400,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            onUpdateBranding(appNameInput, selectedIconKey)
                            brandingMessage = "Branding updated successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Branding", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 6: Permanent History Deletion
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = Rose500)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete All Audit History",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Rose500
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Permanently wipes all local and Cloud Firestore audit logs, sessions, and records.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Slate200),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Permanently Delete All History", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete All History?", color = Slate200, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "This will permanently erase all local lending session records, call records, SMS logs, and delete all user records from Cloud Firestore. This cannot be undone.",
                        color = Slate400,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            onDeleteAllHistory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Slate200)
                    ) {
                        Text("Yes, Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Slate400)
                    }
                },
                containerColor = Navy800
            )
        }
    }
}

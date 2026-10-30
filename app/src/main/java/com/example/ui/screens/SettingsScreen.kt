package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDemoDataEnabled: Boolean,
    onToggleDemoData: (Boolean) -> Unit,
    onChangePasswords: (oldMaster: String, newMaster: String, newSecondary: String, hint: String) -> Boolean,
    onBack: () -> Unit
) {
    var isChangingPassword by remember { mutableStateOf(false) }
    var currentMasterPass by remember { mutableStateOf("") }
    var newMasterPass by remember { mutableStateOf("") }
    var newSecondaryPass by remember { mutableStateOf("") }
    var newHint by remember { mutableStateOf("") }
    var formMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Settings",
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security Status Card
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
                            text = "Authentication Protocol",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dual-Password Encryption Active",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate200)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Rose500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fingerprint Disabled (Owner's Preference)",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Slate200)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Aap ki hidayat ke mutabiq fingerprint se app nahi khulegi taake koi aap ke sote waqt ya zabardasti unseal na kar sake. Sirf 2 secret passwords se hi open hogi.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 16.sp)
                    )
                }
            }

            // Demo / Test Mode Toggle Card
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
                            text = "Emulator Test / Demo Data",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shows realistic simulated call & SMS history if testing on browser emulator without physical SIM.",
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

            // Update Passwords Section
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Amber500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Change Dual Passwords",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    if (!isChangingPassword) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Master aur Secondary password ko badalne ke liye click karein.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isChangingPassword = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Slate200),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Update Passwords")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))

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

            // Security Guidelines Note
            Card(
                colors = CardDefaults.cardColors(containerColor = Navy800),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Phone kisi ko dene se pehle Dashboard se 'Start Lending Session' dabayein. Wapas lene par 'End & Audit' dabane par aap ko exact calls aur SMS ki list mil jayegi.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400, lineHeight = 18.sp)
                    )
                }
            }
        }
    }
}

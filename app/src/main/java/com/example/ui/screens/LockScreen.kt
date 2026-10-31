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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Amber500
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400

@Composable
fun LockScreen(
    customAppName: String = "Device Audit Log",
    isBiometricEnabled: Boolean = true,
    onUnlockWithPin: (pin: String) -> Boolean,
    onBiometricClick: () -> Unit,
    lockoutSeconds: Long,
    securityHint: String,
    errorMessage: String?
) {
    var pinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showHintDialog by remember { mutableStateOf(false) }

    val isLockedOut = lockoutSeconds > 0
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Lock icon badge
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(Navy800, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Device Audit Locked",
                    tint = if (isLockedOut) Rose500 else Cyan400,
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = customAppName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate200
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Owner Protection • Secure Device Audit",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Cyan400,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter Master PIN or use your registered fingerprint to unlock.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate400),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLockedOut) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Security Lockout Active",
                            color = Rose500,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Too many incorrect attempts. Retry in: ${lockoutSeconds}s",
                            color = Slate200,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // PIN Input Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Navy800),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("Master PIN") },
                            placeholder = { Text("Enter 4-digit PIN") },
                            singleLine = true,
                            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (pinInput.isNotBlank()) {
                                        val unlocked = onUnlockWithPin(pinInput)
                                        if (!unlocked) pinInput = ""
                                    }
                                }
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPin = !showPin }) {
                                    Icon(
                                        imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility",
                                        tint = Slate400
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cyan400,
                                unfocusedBorderColor = Navy700,
                                focusedTextColor = Slate200,
                                unfocusedTextColor = Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("unlock_pin_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                color = Rose500,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (pinInput.isNotBlank()) {
                                    val unlocked = onUnlockWithPin(pinInput)
                                    if (!unlocked) pinInput = ""
                                }
                            },
                            enabled = pinInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_button")
                        ) {
                            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlock with PIN",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // Fingerprint Action Button
                        if (isBiometricEnabled) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onBiometricClick,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("biometric_unlock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Fingerprint",
                                    tint = Emerald400
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scan Fingerprint",
                                    color = Slate200,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recovery Hint Option
                if (securityHint.isNotBlank()) {
                    TextButton(
                        onClick = { showHintDialog = true },
                        modifier = Modifier.testTag("show_hint_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Help Hint",
                            tint = Amber500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Forgot PIN? View Hint",
                            color = Amber500,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        // Hint Dialog
        if (showHintDialog) {
            AlertDialog(
                onDismissRequest = { showHintDialog = false },
                title = {
                    Text("Security PIN Hint", color = Slate200, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Your saved recovery hint:",
                            color = Slate400,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Navy700),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = securityHint,
                                color = Cyan400,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHintDialog = false }) {
                        Text("Close", color = Cyan400)
                    }
                },
                containerColor = Navy800
            )
        }
    }
}

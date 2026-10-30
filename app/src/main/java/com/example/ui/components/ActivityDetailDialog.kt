package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.AuditItem
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400

@Composable
fun ActivityDetailDialog(
    item: AuditItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Navy800),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("activity_detail_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                val (title, icon, iconColor) = when (item) {
                    is AuditItem.Call -> Triple("Call Audit Detail", Icons.Default.Phone, Cyan400)
                    is AuditItem.Sms -> Triple("SMS Audit Detail", Icons.Default.Sms, Cyan400)
                    is AuditItem.WhatsApp -> Triple("WhatsApp Audit Detail", Icons.AutoMirrored.Filled.Chat, Color(0xFF25D366))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(iconColor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (item) {
                    is AuditItem.Call -> {
                        val call = item.entry
                        DetailRow("Contact / Name", call.cachedName ?: "Not in Contacts")
                        DetailRow("Phone Number", call.number)
                        DetailRow("Call Type", "${call.type.displayName} (${call.type.urduLabel})")
                        DetailRow("Timestamp", call.formattedTime)
                        DetailRow("Duration (Talk Time)", call.formattedDuration)

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ye call record phone ke Call Log se liya gaya hai. Owner ke password ke baghair ise delete ya chupa nahi ja sakta.",
                                color = Slate400,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    is AuditItem.Sms -> {
                        val sms = item.entry
                        DetailRow("Contact / Address", sms.cachedName ?: sms.address)
                        DetailRow("Type", "${sms.type.displayName} (${sms.type.urduLabel})")
                        DetailRow("Timestamp", sms.formattedTime)

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Message Content:",
                            color = Slate200,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = sms.body.ifBlank { "(Empty message content)" },
                                color = Slate200,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    is AuditItem.WhatsApp -> {
                        val wa = item.entry
                        DetailRow("WhatsApp Contact", wa.senderOrCaller)
                        DetailRow("Activity Type", if (wa.isCall) "WhatsApp Voice/Video Call Event" else "Incoming WhatsApp Message")
                        DetailRow("Timestamp", wa.formattedTime)

                        if (!wa.isCall && wa.body.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Captured Notification Text:",
                                color = Slate200,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Navy900,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = wa.body,
                                    color = Slate200,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "WhatsApp activity Android Notification Listener API ke zariye capture hoti hai jo 100% Android privacy policy ke mutabiq hai.",
                                color = Slate400,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close Record", color = Cyan400)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            color = Slate400,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            color = Slate200,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

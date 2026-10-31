package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardDark
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.util.DeviceOemHelper
import com.example.util.OemDeviceInfo

@Composable
fun OnboardingScreen(
    oemInfo: OemDeviceInfo,
    isBatteryOptimizationIgnored: Boolean,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentPageIndex by remember { mutableIntStateOf(0) }
    val totalPages = 4
    val isLastPage = currentPageIndex == totalPages - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(24.dp)
    ) {
        // Skip Button
        if (!isLastPage) {
            TextButton(
                onClick = onComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .testTag("skip_onboarding_button")
            ) {
                Text(
                    text = "Skip",
                    color = Slate400,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Slide Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = currentPageIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "onboarding_animation"
            ) { page ->
                when (page) {
                    0 -> {
                        // Slide 1: Phone Lending Audit
                        OnboardingSlide(
                            icon = Icons.Default.Phone,
                            iconColor = Cyan400,
                            title = "Device Activity Auditor",
                            subtitle = "Audit calls & SMS when lending your phone",
                            description = "Before handing your device to a friend or coworker, start a 1-tap Lending Session. When you get your phone back, see the exact calls made, call duration, and messages sent."
                        )
                    }
                    1 -> {
                        // Slide 2: Device OEM Recognition & Battery Optimization
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(Navy800, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.size(44.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Device Optimization Check",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate200
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Detected: ${oemInfo.brandDisplayName} (${oemInfo.model})",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald400
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = oemInfo.guidanceTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate200
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = oemInfo.guidanceDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Slate400,
                                            lineHeight = 18.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = { DeviceOemHelper.openBatteryOptimizationSettings(context) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isBatteryOptimizationIgnored) "Optimization Disabled (Active)" else "Allow Unrestricted Background",
                                            color = Emerald400,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Slide 3: PIN & Fingerprint
                        OnboardingSlide(
                            icon = Icons.Default.Fingerprint,
                            iconColor = Cyan400,
                            title = "PIN & Biometric Security",
                            subtitle = "Master PIN + Fingerprint Unlock",
                            description = "Lock the app with a single Master Security PIN. You can also scan your fingerprint for fast, seamless biometric unlocking. Nobody can view or tamper with your history without your authorization."
                        )
                    }
                    3 -> {
                        // Slide 4: Cloud Firestore Sync
                        OnboardingSlide(
                            icon = Icons.Default.CloudDone,
                            iconColor = Emerald400,
                            title = "Cloud Backup & Sync",
                            subtitle = "Never lose your audit records",
                            description = "Sign in with your Google account in Settings to sync all lending sessions, calls, SMS, and WhatsApp audit records directly to your secure Cloud Firestore database."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isSelected = index == currentPageIndex
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (isSelected) 28.dp else 8.dp)
                            .background(
                                color = if (isSelected) Cyan400 else Navy700,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Next / Finish Button
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        currentPageIndex++
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLastPage) Emerald400 else Cyan400,
                    contentColor = Navy900
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_action_button")
            ) {
                if (isLastPage) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Up Master PIN",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlide(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(Navy800, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Slate200
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = iconColor
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate400,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(18.dp)
            )
        }
    }
}

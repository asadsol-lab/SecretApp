package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

data class OemDeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val brandDisplayName: String,
    val guidanceTitle: String,
    val guidanceDescription: String
)

object DeviceOemHelper {

    fun getDeviceInfo(): OemDeviceInfo {
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val rawModel = Build.MODEL.orEmpty()
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val brandDisplayName: String
        val guidanceTitle: String
        val guidanceDescription: String

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                brandDisplayName = "Xiaomi / Redmi / POCO"
                guidanceTitle = "MIUI / HyperOS Autostart & Battery Optimization"
                guidanceDescription = "Xiaomi devices automatically terminate background listeners. Please enable 'Autostart' and set Battery Saver to 'No Restrictions' in App Settings."
            }
            manufacturer.contains("samsung") -> {
                brandDisplayName = "Samsung Galaxy"
                guidanceTitle = "OneUI Deep Sleep & Background Management"
                guidanceDescription = "Samsung devices put inactive apps into Deep Sleeping mode. Please set Battery usage to 'Unrestricted' in App Info."
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                brandDisplayName = "Oppo / Realme (ColorOS)"
                guidanceTitle = "ColorOS Background Execution"
                guidanceDescription = "ColorOS disables background activity by default. Enable 'Allow background activity' and 'Auto-launch' for uninterrupted auditing."
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                brandDisplayName = "Vivo / iQOO (FuntouchOS)"
                guidanceTitle = "FuntouchOS High Background Power Usage"
                guidanceDescription = "Enable 'High background power consumption' and autostart to prevent Android from halting session audit logging."
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                brandDisplayName = "Huawei / Honor (EMUI)"
                guidanceTitle = "EMUI App Launch Protection"
                guidanceDescription = "Go to Battery -> App Launch, turn off 'Manage automatically' and enable 'Auto-launch', 'Secondary launch', and 'Run in background'."
            }
            manufacturer.contains("oneplus") -> {
                brandDisplayName = "OnePlus (OxygenOS)"
                guidanceTitle = "OxygenOS Battery Optimization"
                guidanceDescription = "Set App battery usage to 'Don't optimize' so that lending sessions remain active when the phone screen turns off."
            }
            else -> {
                val capitalized = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
                brandDisplayName = if (capitalized.isNotBlank()) capitalized else "Standard Android"
                guidanceTitle = "Android Background Restrictions"
                guidanceDescription = "To prevent system memory managers from stopping the lending session tracker, allow Unrestricted Battery and background execution."
            }
        }

        return OemDeviceInfo(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = rawModel,
            androidVersion = androidVersion,
            brandDisplayName = brandDisplayName,
            guidanceTitle = guidanceTitle,
            guidanceDescription = guidanceDescription
        )
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                openAppDetailsSettings(context)
            }
        }
    }

    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}

package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.SecurityManager
import com.example.model.WhatsAppEntry

class WhatsAppNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName != "com.whatsapp" && packageName != "com.whatsapp.w4b") {
            return
        }

        val extras = sbn.notification.extras ?: return
        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence("android.title.big")
            ?: "").toString()

        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
            ?: "").toString()

        if (title.isBlank() && text.isBlank()) return

        val isCall = sbn.notification.category == Notification.CATEGORY_CALL ||
                title.contains("call", ignoreCase = true) ||
                text.contains("incoming call", ignoreCase = true) ||
                text.contains("missed call", ignoreCase = true) ||
                text.contains("voice call", ignoreCase = true) ||
                text.contains("video call", ignoreCase = true)

        val entry = WhatsAppEntry(
            id = System.currentTimeMillis() + sbn.id,
            senderOrCaller = title.ifBlank { "WhatsApp Contact" },
            body = text,
            isCall = isCall,
            timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        )

        try {
            val securityManager = SecurityManager(applicationContext)
            securityManager.saveWhatsAppEntry(entry)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}

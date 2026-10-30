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
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val isCall = title.contains("call", ignoreCase = true) ||
                text.contains("incoming call", ignoreCase = true) ||
                text.contains("missed call", ignoreCase = true)

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

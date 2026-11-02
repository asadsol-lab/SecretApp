package com.example.service

import android.app.Notification
import android.content.Intent
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

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence("android.conversationTitle")
            ?: extras.getCharSequence("android.title.big")
            ?: "").toString().trim()

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        val textBody = if (!textLines.isNullOrEmpty()) {
            textLines.filterNotNull().joinToString("\n") { it.toString() }
        } else {
            (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
                ?: "").toString().trim()
        }

        // Avoid capturing empty system notifications (e.g. WhatsApp Web is active, backup in progress)
        if (title.isBlank() && textBody.isBlank()) return
        if (title.contains("WhatsApp Web", ignoreCase = true) ||
            title.contains("Checking for new messages", ignoreCase = true) ||
            textBody.contains("Backup in progress", ignoreCase = true) ||
            textBody.contains("Looking for new messages", ignoreCase = true)) {
            return
        }

        val category = notification.category
        val isCall = category == Notification.CATEGORY_CALL ||
                title.contains("call", ignoreCase = true) ||
                textBody.contains("call", ignoreCase = true) ||
                textBody.contains("ringing", ignoreCase = true) ||
                textBody.contains("calling", ignoreCase = true)

        val sender = title.ifBlank { "WhatsApp Contact" }
        val bodyContent = if (isCall && textBody.isBlank()) {
            "WhatsApp Voice / Video Call"
        } else {
            textBody.ifBlank { "Incoming Notification" }
        }

        val postTime = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()
        val entry = WhatsAppEntry(
            id = postTime + sbn.id.toLong(),
            senderOrCaller = sender,
            body = bodyContent,
            isCall = isCall,
            timestamp = postTime
        )

        try {
            val securityManager = SecurityManager(applicationContext)
            securityManager.saveWhatsAppEntry(entry)

            // Broadcast to MainActivity / AuditViewModel to refresh UI immediately
            val broadcastIntent = Intent("com.example.AUDIT_DATA_CHANGED").apply {
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(broadcastIntent)
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}

package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LendingSession(
    val id: String,
    val borrowerName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String = "",
    val callsRecordedCount: Int = 0,
    val smsRecordedCount: Int = 0
) {
    val isActive: Boolean
        get() = endTime == null

    val formattedStartTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(startTime))
        }

    val formattedEndTime: String
        get() {
            return if (endTime != null) {
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                sdf.format(Date(endTime))
            } else {
                "Active (Ongoing)"
            }
        }

    val durationText: String
        get() {
            val end = endTime ?: System.currentTimeMillis()
            val diffMs = (end - startTime).coerceAtLeast(0)
            val minutes = diffMs / (1000 * 60)
            val hours = minutes / 60
            val remMinutes = minutes % 60
            return if (hours > 0) {
                "${hours}h ${remMinutes}m"
            } else {
                "${remMinutes}m"
            }
        }
}

sealed class AuditItem(val timestamp: Long) {
    data class Call(val entry: CallEntry) : AuditItem(entry.timestamp)
    data class Sms(val entry: SmsEntry) : AuditItem(entry.timestamp)
}

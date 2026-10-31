package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallType(val displayName: String) {
    OUTGOING("Outgoing Call"),
    INCOMING("Incoming Call"),
    MISSED("Missed Call"),
    REJECTED("Rejected Call"),
    UNKNOWN("Unknown Call")
}

data class CallEntry(
    val id: Long,
    val number: String,
    val cachedName: String?,
    val type: CallType,
    val timestamp: Long,
    val durationSeconds: Long
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDuration: String
        get() {
            if (durationSeconds <= 0) {
                return if (type == CallType.MISSED) "0s (Missed)" else "0s"
            }
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return if (minutes > 0) {
                "${minutes}m ${seconds}s"
            } else {
                "${seconds}s"
            }
        }

    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() } ?: number
}

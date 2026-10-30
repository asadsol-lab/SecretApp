package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SmsType(val displayName: String, val urduLabel: String) {
    SENT("Sent SMS", "Sent (Bheja gaya)"),
    RECEIVED("Received SMS", "Received (Mausool hua)"),
    DRAFT("Draft", "Draft")
}

data class SmsEntry(
    val id: Long,
    val address: String,
    val cachedName: String?,
    val body: String,
    val type: SmsType,
    val timestamp: Long
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val displayTitle: String
        get() = cachedName?.takeIf { it.isNotBlank() } ?: address
}

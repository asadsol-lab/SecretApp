package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WhatsAppEntry(
    val id: Long,
    val senderOrCaller: String,
    val body: String,
    val isCall: Boolean,
    val timestamp: Long
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val displayTitle: String
        get() = senderOrCaller.ifBlank { "WhatsApp Contact" }
}

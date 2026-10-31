package com.example.data

import com.example.model.CallEntry
import com.example.model.CallType
import com.example.model.SmsEntry
import com.example.model.SmsType

object SampleDataProvider {

    fun getSampleCalls(sinceTimestamp: Long = 0L): List<CallEntry> {
        val now = System.currentTimeMillis()
        val all = listOf(
            CallEntry(
                id = 101L,
                number = "+92 301 5550192",
                cachedName = "Ahmed Khan (Colleague)",
                type = CallType.OUTGOING,
                timestamp = now - (12 * 60 * 1000L), // 12 mins ago
                durationSeconds = 245 // 4m 5s
            ),
            CallEntry(
                id = 102L,
                number = "+92 333 4441234",
                cachedName = "Zainab Ali",
                type = CallType.INCOMING,
                timestamp = now - (25 * 60 * 1000L), // 25 mins ago
                durationSeconds = 118 // 1m 58s
            ),
            CallEntry(
                id = 103L,
                number = "+92 312 9988776",
                cachedName = "Delivery Rider",
                type = CallType.MISSED,
                timestamp = now - (48 * 60 * 1000L), // 48 mins ago
                durationSeconds = 0
            ),
            CallEntry(
                id = 104L,
                number = "+92 300 1234567",
                cachedName = "Uncle Tariq",
                type = CallType.OUTGOING,
                timestamp = now - (2 * 60 * 60 * 1000L), // 2 hours ago
                durationSeconds = 480 // 8 mins
            ),
            CallEntry(
                id = 105L,
                number = "+92 345 8765432",
                cachedName = null,
                type = CallType.REJECTED,
                timestamp = now - (4 * 60 * 60 * 1000L), // 4 hours ago
                durationSeconds = 0
            ),
            CallEntry(
                id = 106L,
                number = "+92 302 1122334",
                cachedName = "Hamza Office",
                type = CallType.INCOMING,
                timestamp = now - (18 * 60 * 60 * 1000L), // 18 hours ago
                durationSeconds = 312 // 5m 12s
            )
        )

        return if (sinceTimestamp > 0L) {
            all.filter { it.timestamp >= sinceTimestamp }
        } else {
            all
        }
    }

    fun getSampleSms(sinceTimestamp: Long = 0L): List<SmsEntry> {
        val now = System.currentTimeMillis()
        val all = listOf(
            SmsEntry(
                id = 201L,
                address = "+92 301 5550192",
                cachedName = "Ahmed Khan",
                body = "I have emailed the files, please review them.",
                type = SmsType.SENT,
                timestamp = now - (15 * 60 * 1000L) // 15 mins ago
            ),
            SmsEntry(
                id = 202L,
                address = "+92 301 5550192",
                cachedName = "Ahmed Khan",
                body = "Sounds good, thanks a lot!",
                type = SmsType.RECEIVED,
                timestamp = now - (14 * 60 * 1000L) // 14 mins ago
            ),
            SmsEntry(
                id = 203L,
                address = "HBL-Alerts",
                cachedName = "HBL Bank",
                body = "Dear Customer, an OTP of 849201 has been generated for your transaction.",
                type = SmsType.RECEIVED,
                timestamp = now - (35 * 60 * 1000L) // 35 mins ago
            ),
            SmsEntry(
                id = 204L,
                address = "+92 333 4441234",
                cachedName = "Zainab Ali",
                body = "Please share the meeting location with me.",
                type = SmsType.RECEIVED,
                timestamp = now - (3 * 60 * 60 * 1000L) // 3 hours ago
            ),
            SmsEntry(
                id = 205L,
                address = "+92 333 4441234",
                cachedName = "Zainab Ali",
                body = "Sending it to you right now.",
                type = SmsType.SENT,
                timestamp = now - (2 * 60 * 60 * 1000L + 55 * 60 * 1000L)
            )
        )

        return if (sinceTimestamp > 0L) {
            all.filter { it.timestamp >= sinceTimestamp }
        } else {
            all
        }
    }
}

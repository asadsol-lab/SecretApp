package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.model.SmsEntry
import com.example.model.SmsType

class SmsLogRepository(private val context: Context) {

    fun getSmsLogs(sinceTimestamp: Long = 0L): List<SmsEntry> {
        val smsList = mutableListOf<SmsEntry>()

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val selection = if (sinceTimestamp > 0L) {
            "${Telephony.Sms.DATE} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp > 0L) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(Telephony.Sms._ID)
                val addressIdx = it.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = it.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = it.getColumnIndex(Telephony.Sms.DATE)
                val typeIdx = it.getColumnIndex(Telephony.Sms.TYPE)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val address = if (addressIdx != -1) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val rawType = if (typeIdx != -1) it.getInt(typeIdx) else 1

                    val smsType = when (rawType) {
                        Telephony.Sms.MESSAGE_TYPE_SENT -> SmsType.SENT
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> SmsType.RECEIVED
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> SmsType.DRAFT
                        else -> SmsType.RECEIVED
                    }

                    smsList.add(
                        SmsEntry(
                            id = id,
                            address = address,
                            cachedName = null,
                            body = body,
                            type = smsType,
                            timestamp = date
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or restricted
        } catch (_: Exception) {
            // Other provider errors
        }

        return smsList
    }
}

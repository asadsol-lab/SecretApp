package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.example.model.SmsEntry
import com.example.model.SmsType
import java.util.concurrent.ConcurrentHashMap

class SmsLogRepository(private val context: Context) {

    companion object {
        // Shared contact cache across SMS queries to prevent slow ContentResolver lookups
        private val contactCache = ConcurrentHashMap<String, String>()
    }

    fun getSmsLogs(sinceTimestamp: Long = 0L, limit: Int = 200): List<SmsEntry> {
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

        val contentUri = try {
            Telephony.Sms.CONTENT_URI
        } catch (_: Exception) {
            Uri.parse("content://sms")
        }

        try {
            val cursor = context.contentResolver.query(
                contentUri,
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

                var count = 0
                while (it.moveToNext() && count < limit) {
                    count++
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val address = if (addressIdx != -1) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val rawType = if (typeIdx != -1) it.getInt(typeIdx) else 1

                    var contactName: String? = null
                    if (address.isNotBlank() && address != "Unknown") {
                        if (contactCache.containsKey(address)) {
                            contactName = contactCache[address]
                        } else {
                            val resolved = lookupContactName(address)
                            if (resolved != null) {
                                contactCache[address] = resolved
                                contactName = resolved
                            }
                        }
                    }

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
                            cachedName = contactName,
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

    private fun lookupContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (idx != -1) it.getString(idx) else null
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}

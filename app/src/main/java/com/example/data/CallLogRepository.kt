package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.model.CallEntry
import com.example.model.CallType
import java.util.concurrent.ConcurrentHashMap

class CallLogRepository(private val context: Context) {

    companion object {
        // Shared contact cache across queries to prevent redundant ContentResolver lookups
        private val contactCache = ConcurrentHashMap<String, String>()
    }

    fun getCallLogs(sinceTimestamp: Long = 0L, limit: Int = 200): List<CallEntry> {
        val callList = mutableListOf<CallEntry>()

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        val selection = if (sinceTimestamp > 0L) {
            "${CallLog.Calls.DATE} >= ?"
        } else null

        val selectionArgs = if (sinceTimestamp > 0L) {
            arrayOf(sinceTimestamp.toString())
        } else null

        val sortOrder = "${CallLog.Calls.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = it.getColumnIndex(CallLog.Calls.DURATION)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    count++
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "Unknown" else "Unknown"
                    var name = if (nameIdx != -1) it.getString(nameIdx) else null
                    val rawType = if (typeIdx != -1) it.getInt(typeIdx) else 0
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val duration = if (durationIdx != -1) it.getLong(durationIdx) else 0L

                    // Lookup contact name from phonebook if cachedName is empty
                    if (name.isNullOrBlank() && number.isNotBlank() && number != "Unknown") {
                        if (contactCache.containsKey(number)) {
                            name = contactCache[number]
                        } else {
                            val resolved = lookupContactName(number)
                            if (resolved != null) {
                                contactCache[number] = resolved
                                name = resolved
                            }
                        }
                    }

                    val callType = when (rawType) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                        else -> CallType.UNKNOWN
                    }

                    callList.add(
                        CallEntry(
                            id = id,
                            number = number,
                            cachedName = name,
                            type = callType,
                            timestamp = date,
                            durationSeconds = duration
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or restricted
        } catch (_: Exception) {
            // Other provider errors
        }

        return callList
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

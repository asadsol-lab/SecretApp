package com.example.data

import android.content.Context
import android.provider.CallLog
import com.example.model.CallEntry
import com.example.model.CallType

class CallLogRepository(private val context: Context) {

    fun getCallLogs(sinceTimestamp: Long = 0L): List<CallEntry> {
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

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val number = if (numberIdx != -1) it.getString(numberIdx) ?: "Unknown" else "Unknown"
                    val name = if (nameIdx != -1) it.getString(nameIdx) else null
                    val rawType = if (typeIdx != -1) it.getInt(typeIdx) else 0
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else 0L
                    val duration = if (durationIdx != -1) it.getLong(durationIdx) else 0L

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
}

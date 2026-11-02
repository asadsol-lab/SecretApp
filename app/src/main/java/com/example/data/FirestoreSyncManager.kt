package com.example.data

import android.content.Context
import com.example.model.CallEntry
import com.example.model.LendingSession
import com.example.model.SmsEntry
import com.example.model.WhatsAppEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(private val context: Context) {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    suspend fun syncSession(session: LendingSession): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val sessionDoc = firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(session.id)

            val data = hashMapOf(
                "id" to session.id,
                "borrowerName" to session.borrowerName,
                "startTime" to session.startTime,
                "endTime" to (session.endTime ?: 0L),
                "notes" to session.notes,
                "callsRecordedCount" to session.callsRecordedCount,
                "smsRecordedCount" to session.smsRecordedCount,
                "whatsappRecordedCount" to session.whatsappRecordedCount,
                "syncedAt" to FieldValue.serverTimestamp()
            )

            sessionDoc.set(data, SetOptions.merge()).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun syncAllRecords(
        sessions: List<LendingSession>,
        calls: List<CallEntry>,
        sms: List<SmsEntry>,
        whatsapp: List<WhatsAppEntry>
    ): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            val userRef = firestore.collection("users").document(uid)

            // Sync sessions
            for (s in sessions.take(50)) {
                userRef.collection("sessions").document(s.id).set(
                    hashMapOf(
                        "id" to s.id,
                        "borrowerName" to s.borrowerName,
                        "startTime" to s.startTime,
                        "endTime" to (s.endTime ?: 0L),
                        "callsRecordedCount" to s.callsRecordedCount,
                        "smsRecordedCount" to s.smsRecordedCount,
                        "whatsappRecordedCount" to s.whatsappRecordedCount,
                        "syncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // Sync calls
            for (c in calls.take(200)) {
                userRef.collection("calls").document(c.id.toString()).set(
                    hashMapOf(
                        "id" to c.id,
                        "number" to c.number,
                        "cachedName" to (c.cachedName ?: ""),
                        "type" to c.type.name,
                        "timestamp" to c.timestamp,
                        "durationSeconds" to c.durationSeconds,
                        "syncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // Sync SMS
            for (m in sms.take(200)) {
                userRef.collection("sms").document(m.id.toString()).set(
                    hashMapOf(
                        "id" to m.id,
                        "address" to m.address,
                        "cachedName" to (m.cachedName ?: ""),
                        "body" to m.body,
                        "type" to m.type.name,
                        "timestamp" to m.timestamp,
                        "syncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            // Sync WhatsApp
            for (w in whatsapp.take(200)) {
                userRef.collection("whatsapp").document(w.id.toString()).set(
                    hashMapOf(
                        "id" to w.id,
                        "senderOrCaller" to w.senderOrCaller,
                        "body" to w.body,
                        "isCall" to w.isCall,
                        "timestamp" to w.timestamp,
                        "syncedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun deleteAllUserData(): Boolean {
        val uid = auth.currentUser?.uid ?: return true
        return try {
            val userRef = firestore.collection("users").document(uid)

            val collections = listOf("sessions", "calls", "sms", "whatsapp")
            for (col in collections) {
                val snapshot = userRef.collection(col).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            }
            userRef.delete().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}

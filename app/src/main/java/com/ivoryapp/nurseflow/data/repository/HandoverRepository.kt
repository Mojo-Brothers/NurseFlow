package com.ivoryapp.nurseflow.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ivoryapp.nurseflow.data.model.HandoverPriority
import com.ivoryapp.nurseflow.data.model.PatientHandover
import com.ivoryapp.nurseflow.data.model.ShiftSession
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.util.VitalSignAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class HandoverRepository(
    private val patientRepository: PatientRepository,
    private val vitalSignRepository: VitalSignRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "HandoverRepository"

    // Fungsi baru untuk mengirim handover per pasien dari Perawat A ke Perawat B
    suspend fun sendPatientHandover(patient: Patient, toUid: String, fromName: String) {
        val fromUid = auth.currentUser?.uid ?: return
        
        val handoverRequest = hashMapOf(
            "patientId" to patient.id,
            "patientName" to patient.name,
            "roomNumber" to patient.roomNumber,
            "fromUid" to fromUid,
            "fromName" to fromName,
            "toUid" to toUid,
            "status" to "PENDING",
            "timestamp" to Timestamp.now()
        )

        try {
            firestore.collection("handover_requests").add(handoverRequest).await()
            Log.d(TAG, "Handover request sent for patient: ${patient.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending handover: ${e.message}")
            throw e
        }
    }

    // Fungsi untuk menerima (Accept) handover oleh Perawat B
    suspend fun acceptHandoverRequest(requestId: String, patientId: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.runBatch { batch ->
                // 1. Ubah ownerUid pasien
                val patientRef = firestore.collection("patients").document(patientId.toString())
                batch.update(patientRef, "ownerUid", uid, "updatedAt", System.currentTimeMillis())

                // 2. Tandai request sebagai ACCEPTED
                val requestRef = firestore.collection("handover_requests").document(requestId)
                batch.update(requestRef, "status", "ACCEPTED")
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting handover: ${e.message}")
            throw e
        }
    }

    // Fungsi untuk menolak (Reject) handover
    suspend fun rejectHandoverRequest(requestId: String) {
        try {
            firestore.collection("handover_requests").document(requestId)
                .update("status", "REJECTED")
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting handover: ${e.message}")
        }
    }

    // --- Shift Session Methods ---

    suspend fun startNewSession(shiftType: String, fromColleagueUid: String? = null) {
        val uid = auth.currentUser?.uid ?: return
        val session = ShiftSession(
            shiftType = shiftType,
            fromUid = fromColleagueUid ?: "",
            createdBy = uid,
            status = "ACTIVE"
        )
        try {
            firestore.collection("shift_sessions").add(session).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting session: ${e.message}")
            throw e
        }
    }

    suspend fun getActiveSession(): ShiftSession? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = firestore.collection("shift_sessions")
                .whereEqualTo("createdBy", uid)
                .whereEqualTo("status", "ACTIVE")
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) return null
            snapshot.documents[0].toObject(ShiftSession::class.java)?.copy(id = snapshot.documents[0].id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun completeSession(sessionId: String) {
        try {
            firestore.collection("shift_sessions").document(sessionId)
                .update("status", "COMPLETED", "endTime", Timestamp.now())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error completing session: ${e.message}")
        }
    }

    // --- Handover Items Methods ---

    suspend fun getHandoverItems(sessionId: String): List<PatientHandover> {
        return try {
            val snapshot = firestore.collection("handover_items")
                .whereEqualTo("sessionId", sessionId)
                .get()
                .await()
            snapshot.documents.map { doc ->
                doc.toObject(PatientHandover::class.java)!!.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting handover items: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateHandoverItem(item: PatientHandover) {
        try {
            if (item.id.isNotEmpty()) {
                firestore.collection("handover_items").document(item.id).set(item).await()
            } else {
                firestore.collection("handover_items").add(item).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating handover item: ${e.message}")
        }
    }

    suspend fun getColleagues(): List<Pair<String, String>> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val connections = firestore.collection("connections")
                .whereArrayContains("members", uid)
                .get()
                .await()
            
            val colleagues = mutableListOf<Pair<String, String>>()
            for (doc in connections) {
                val members = doc.get("members") as? List<String>
                val colleagueUid = members?.firstOrNull { it != uid }
                if (colleagueUid != null) {
                    val userDoc = firestore.collection("users").document(colleagueUid).get().await()
                    val name = userDoc.getString("name") ?: "Nurse"
                    colleagues.add(colleagueUid to name)
                }
            }
            colleagues
        } catch (e: Exception) {
            emptyList()
        }
    }
}

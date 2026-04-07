package com.ivoryapp.nurseflow.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ivoryapp.nurseflow.data.model.Handover
import com.ivoryapp.nurseflow.data.model.HandoverTask
import com.ivoryapp.nurseflow.data.model.Patient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HandoverRepository(
    private val patientRepository: PatientRepository,
    private val vitalSignRepository: VitalSignRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "HandoverRepository"

    // MENGIRIM HANDOVER BESERTA TASK LANJUTAN
    suspend fun createHandoverRequest(patient: Patient, toUid: String, fromName: String, tasks: List<String>) {
        val fromUid = auth.currentUser?.uid ?: return
        
        val handoverRef = firestore.collection("handovers").document()
        val handover = Handover(
            id = handoverRef.id,
            patientId = patient.id,
            patientName = patient.name,
            fromUid = fromUid,
            fromName = fromName,
            toUid = toUid,
            status = "PENDING",
            timestamp = Timestamp.now()
        )

        try {
            firestore.runTransaction { transaction ->
                // 1. Simpan Dokumen Handover Utama
                transaction.set(handoverRef, handover)

                // 2. Simpan Daftar Tugas Lanjutan (Checklist)
                tasks.forEach { taskTitle ->
                    if (taskTitle.isNotBlank()) {
                        val taskRef = firestore.collection("handover_tasks").document()
                        val task = HandoverTask(
                            id = taskRef.id,
                            handoverId = handoverRef.id,
                            title = taskTitle,
                            isCompleted = false,
                            createdBy = fromUid,
                            timestamp = Timestamp.now()
                        )
                        transaction.set(taskRef, task)
                    }
                }

                // 3. Simpan Notifikasi untuk Perawat B
                val notificationRef = firestore.collection("notifications").document()
                val notificationData = hashMapOf(
                    "userId" to toUid,
                    "title" to "Permintaan Handover",
                    "message" to "$fromName ingin menyerahkan tanggung jawab pasien ${patient.name}. Ada ${tasks.size} tugas lanjutan.",
                    "type" to "HANDOVER_REQUEST",
                    "fromUid" to fromUid,
                    "fromName" to fromName,
                    "handoverId" to handoverRef.id,
                    "patientId" to patient.id,
                    "patientName" to patient.name,
                    "taskCount" to tasks.size,
                    "status" to "pending",
                    "timestamp" to Timestamp.now(),
                    "isRead" to false
                )
                transaction.set(notificationRef, notificationData)
            }.await()
            Log.d(TAG, "Handover request with tasks created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating handover: ${e.message}")
            throw e
        }
    }

    // TERIMA HANDOVER (Atomic Transaction: Pindah Ownership + Update Status)
    suspend fun acceptHandoverRequest(handoverId: String, patientId: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            firestore.runTransaction { transaction ->
                val handoverRef = firestore.collection("handovers").document(handoverId)
                val patientRef = firestore.collection("patients").document(patientId.toString())

                // Update Status Handover
                transaction.update(handoverRef, "status", "ACCEPTED", "acceptedAt", Timestamp.now())
                
                // PINDAH TANGGUNG JAWAB (Ownership Transfer)
                transaction.update(patientRef, "ownerUid", uid, "lastHandoverId", handoverId)
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting handover: ${e.message}")
            throw e
        }
    }

    suspend fun rejectHandoverRequest(handoverId: String) {
        try {
            firestore.collection("handovers").document(handoverId)
                .update("status", "REJECTED")
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting handover: ${e.message}")
        }
    }

    // Ambil daftar handover aktif (ACCEPTED) untuk user saat ini
    fun getActiveHandovers(): Flow<List<Handover>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("handovers")
            .whereIn("status", listOf("ACCEPTED", "PENDING"))
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed: $e")
                    return@addSnapshotListener
                }

                val handovers = snapshot?.documents?.mapNotNull { it.toObject(Handover::class.java) } ?: emptyList()
                val filtered = handovers.filter { it.fromUid == uid || it.toUid == uid }
                trySend(filtered)
            }
        
        awaitClose { listener.remove() }
    }

    // Realtime tasks untuk sebuah handover
    fun getHandoverTasks(handoverId: String): Flow<List<HandoverTask>> = callbackFlow {
        val listener = firestore.collection("handover_tasks")
            .whereEqualTo("handoverId", handoverId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val tasks = snapshot?.documents?.mapNotNull { it.toObject(HandoverTask::class.java) } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleTask(taskId: String, isCompleted: Boolean, handoverId: String) {
        val uid = auth.currentUser?.uid ?: return
        val name = auth.currentUser?.displayName ?: "Perawat"
        
        try {
            firestore.runTransaction { transaction ->
                val taskRef = firestore.collection("handover_tasks").document(taskId)
                val handoverRef = firestore.collection("handovers").document(handoverId)
                
                val taskDoc = transaction.get(taskRef)
                val taskTitle = taskDoc.getString("title") ?: "Tugas"
                val handoverDoc = transaction.get(handoverRef)
                val fromUid = handoverDoc.getString("fromUid") ?: return@runTransaction
                
                // 1. Update status task
                transaction.update(taskRef, "isCompleted", isCompleted)
                
                // 2. Kirim notifikasi ke Perawat A jika task selesai
                if (isCompleted) {
                    val notifRef = firestore.collection("notifications").document()
                    val notifData = hashMapOf(
                        "userId" to fromUid,
                        "title" to "Update Tugas Handover",
                        "message" to "$taskTitle sudah dilakukan oleh $name",
                        "type" to "SYSTEM",
                        "timestamp" to Timestamp.now(),
                        "isRead" to false
                    )
                    transaction.set(notifRef, notifData)
                }
            }.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling task: ${e.message}")
        }
    }

    suspend fun completeHandover(handoverId: String) {
        try {
            firestore.collection("handovers").document(handoverId)
                .update("status", "COMPLETED")
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error completing handover: ${e.message}")
        }
    }

    suspend fun getColleagues(): List<Pair<String, String>> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("connections")
                .whereArrayContains("members", uid)
                .get()
                .await()

            val partnerIds = snapshot.documents.mapNotNull { doc ->
                val members = doc.get("members") as? List<*>
                members?.filterIsInstance<String>()?.firstOrNull { it != uid }
            }

            if (partnerIds.isEmpty()) return emptyList()

            val colleagues = mutableListOf<Pair<String, String>>()
            for (id in partnerIds) {
                val doc = firestore.collection("users").document(id).get().await()
                if (doc.exists()) {
                    val name = doc.getString("name") ?: "Nurse"
                    colleagues.add(id to name)
                }
            }
            colleagues.sortedBy { it.second }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching colleagues: ${e.message}")
            emptyList()
        }
    }
}

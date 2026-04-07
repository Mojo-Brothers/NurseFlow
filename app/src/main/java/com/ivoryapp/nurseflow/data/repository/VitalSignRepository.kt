package com.ivoryapp.nurseflow.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ivoryapp.nurseflow.data.local.VitalSignDao
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.ui.notification.NotificationType
import com.ivoryapp.nurseflow.util.VitalSignAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class VitalSignRepository(private val vitalSignDao: VitalSignDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "VitalSignRepository"

    fun getVitalSignsForPatient(patientId: Int): Flow<List<VitalSign>> =
        vitalSignDao.getVitalSignsForPatient(patientId)

    suspend fun insert(vitalSign: VitalSign) {
        val currentUser = auth.currentUser
        val vitalWithUser = if (currentUser != null) {
            vitalSign.copy(createdBy = currentUser.uid)
        } else {
            vitalSign
        }
        
        val localId = vitalSignDao.insertVitalSign(vitalWithUser)
        
        if (currentUser != null) {
            val vitalWithId = vitalWithUser.copy(id = localId.toInt())
            try {
                syncVitalToFirestore(vitalWithId)
                // Check for clinical alerts after insert
                checkAndCreateClinicalAlert(vitalWithId, currentUser.uid)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync to Firestore: ${e.message}")
            }
        }
    }

    private suspend fun checkAndCreateClinicalAlert(vital: VitalSign, uid: String) {
        val score = VitalSignAnalyzer.calculateNEWS2(vital)
        if (score >= 5) {
            val alertTitle = if (score >= 7) "🔴 CRITICAL ALERT" else "🟡 CLINICAL WARNING"
            val alertMessage = "Pasien memerlukan perhatian segera. Skor NEWS2: $score"
            
            val notificationData = hashMapOf(
                "userId" to uid,
                "title" to alertTitle,
                "message" to alertMessage,
                "type" to NotificationType.CLINICAL_ALERT.name,
                "patientId" to vital.patientId,
                "timestamp" to Timestamp.now(),
                "isRead" to false
            )
            
            try {
                firestore.collection("notifications").add(notificationData).await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create notification: ${e.message}")
            }
        }
    }

    suspend fun update(vitalSign: VitalSign) {
        vitalSignDao.updateVitalSign(vitalSign)
        try {
            syncVitalToFirestore(vitalSign)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update to Firestore: ${e.message}")
        }
    }

    suspend fun delete(vitalSign: VitalSign) {
        vitalSignDao.deleteVitalSign(vitalSign)
        try {
            firestore.collection("vital_signs").document(vitalSign.id.toString()).delete().await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete from Firestore: ${e.message}")
        }
    }

    private suspend fun syncVitalToFirestore(vital: VitalSign) {
        val vitalData = hashMapOf(
            "id" to vital.id,
            "patientId" to vital.patientId,
            "systolic" to vital.systolic,
            "diastolic" to vital.diastolic,
            "pulse" to vital.pulse,
            "temperature" to vital.temperature,
            "respiration" to vital.respiration,
            "spo2" to vital.spo2,
            "painScale" to vital.painScale,
            "consciousness" to vital.consciousness,
            "createdBy" to vital.createdBy,
            "timestamp" to vital.timestamp
        )

        try {
            firestore.collection("vital_signs")
                .document(vital.id.toString())
                .set(vitalData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
             Log.e(TAG, "Firestore set failed: ${e.message}")
             throw e
        }
    }

    suspend fun getColleagueVitalSigns(patientId: Int): List<VitalSign> {
        return try {
            val snapshot = firestore.collection("vital_signs")
                .whereEqualTo("patientId", patientId)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                VitalSign(
                    id = (doc.getLong("id") ?: 0).toInt(),
                    patientId = (doc.getLong("patientId") ?: 0).toInt(),
                    systolic = (doc.getLong("systolic") ?: 0).toInt(),
                    diastolic = (doc.getLong("diastolic") ?: 0).toInt(),
                    pulse = (doc.getLong("pulse") ?: 0).toInt(),
                    temperature = doc.getDouble("temperature") ?: 0.0,
                    respiration = (doc.getLong("respiration") ?: 0).toInt(),
                    spo2 = doc.getLong("spo2")?.toInt(),
                    painScale = doc.getLong("painScale")?.toInt(),
                    consciousness = doc.getString("consciousness") ?: "Alert",
                    createdBy = doc.getString("createdBy"),
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting colleague vitals: ${e.message}")
            emptyList()
        }
    }
}

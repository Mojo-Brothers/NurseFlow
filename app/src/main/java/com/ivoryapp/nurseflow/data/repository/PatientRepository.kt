package com.ivoryapp.nurseflow.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ivoryapp.nurseflow.data.local.PatientDao
import com.ivoryapp.nurseflow.data.model.Patient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PatientRepository(private val patientDao: PatientDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()

    fun getPatientById(id: Int): Flow<Patient?> {
        return patientDao.getPatientById(id)
    }

    suspend fun insert(patient: Patient) {
        // 1. Simpan Lokal
        val localId = patientDao.insertPatient(patient)
        
        // 2. Sinkron ke Firestore jika ada user login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val patientWithId = patient.copy(id = localId.toInt())
            syncPatientToFirestore(patientWithId, currentUser.uid)
        }
    }

    private suspend fun syncPatientToFirestore(patient: Patient, ownerUid: String) {
        val patientData = hashMapOf(
            "id" to patient.id,
            "name" to patient.name,
            "age" to patient.age,
            "dateOfBirth" to patient.dateOfBirth,
            "roomNumber" to patient.roomNumber,
            "conditionBrief" to patient.conditionBrief,
            "ownerUid" to ownerUid,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("patients")
            .document(patient.id.toString())
            .set(patientData, SetOptions.merge())
            .await()
    }

    suspend fun delete(patient: Patient) {
        patientDao.deletePatient(patient)
        firestore.collection("patients").document(patient.id.toString()).delete().await()
    }
    
    suspend fun getColleaguePatients(colleagueUid: String): List<Patient> {
        return try {
            val snapshot = firestore.collection("patients")
                .whereEqualTo("ownerUid", colleagueUid)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                Patient(
                    id = (doc.getLong("id") ?: 0).toInt(),
                    name = doc.getString("name") ?: "",
                    age = (doc.getLong("age") ?: 0).toInt(),
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    roomNumber = doc.getString("roomNumber") ?: "",
                    conditionBrief = doc.getString("conditionBrief") ?: ""
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

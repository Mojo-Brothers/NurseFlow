package com.ivoryapp.nurseflow.data.repository

import com.ivoryapp.nurseflow.data.local.PatientDao
import com.ivoryapp.nurseflow.data.model.Patient
import kotlinx.coroutines.flow.Flow

class PatientRepository(private val patientDao: PatientDao) {
    val allPatients: Flow<List<Patient>> = patientDao.getAllPatients()

    fun getPatientById(id: Int): Flow<Patient?> {
        return patientDao.getPatientById(id)
    }

    suspend fun insert(patient: Patient) {
        patientDao.insertPatient(patient)
    }

    suspend fun delete(patient: Patient) {
        patientDao.deletePatient(patient)
    }
}

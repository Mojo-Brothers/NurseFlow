package com.ivoryapp.nurseflow

import android.app.Application
import com.ivoryapp.nurseflow.data.local.AppDatabase
import com.ivoryapp.nurseflow.data.repository.HandoverRepository
import com.ivoryapp.nurseflow.data.repository.PatientRepository
import com.ivoryapp.nurseflow.data.repository.TaskRepository
import com.ivoryapp.nurseflow.data.repository.VitalSignRepository

class NurseFlowApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val patientRepository: PatientRepository by lazy { PatientRepository(database.patientDao()) }
    val vitalSignRepository: VitalSignRepository by lazy { VitalSignRepository(database.vitalSignDao()) }
    val handoverRepository: HandoverRepository by lazy { 
        HandoverRepository(patientRepository, vitalSignRepository) 
    }
}

package com.ivoryapp.nurseflow.data.local

import androidx.room.*
import com.ivoryapp.nurseflow.data.model.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient)

    @Delete
    suspend fun deletePatient(patient: Patient)
}

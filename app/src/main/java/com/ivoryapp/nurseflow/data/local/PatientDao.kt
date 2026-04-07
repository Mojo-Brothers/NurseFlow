package com.ivoryapp.nurseflow.data.local

import androidx.room.*
import com.ivoryapp.nurseflow.data.model.Patient
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatients(): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE id = :id")
    fun getPatientById(id: Int): Flow<Patient?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Delete
    suspend fun deletePatient(patient: Patient)
}

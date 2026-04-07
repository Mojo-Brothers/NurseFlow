package com.ivoryapp.nurseflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ivoryapp.nurseflow.data.model.VitalSign
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSignDao {
    @Query("SELECT * FROM vital_signs WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getVitalSignsForPatient(patientId: Int): Flow<List<VitalSign>>

    @Insert
    suspend fun insertVitalSign(vitalSign: VitalSign)
}

package com.ivoryapp.nurseflow.data.local

import androidx.room.*
import com.ivoryapp.nurseflow.data.model.VitalSign
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalSignDao {
    @Query("SELECT * FROM vital_signs WHERE patientId = :patientId ORDER BY timestamp DESC")
    fun getVitalSignsForPatient(patientId: Int): Flow<List<VitalSign>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitalSign(vitalSign: VitalSign)

    @Update
    suspend fun updateVitalSign(vitalSign: VitalSign)

    @Delete
    suspend fun deleteVitalSign(vitalSign: VitalSign)
}

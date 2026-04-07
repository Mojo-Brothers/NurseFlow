package com.ivoryapp.nurseflow.data.repository

import com.ivoryapp.nurseflow.data.local.VitalSignDao
import com.ivoryapp.nurseflow.data.model.VitalSign
import kotlinx.coroutines.flow.Flow

class VitalSignRepository(private val vitalSignDao: VitalSignDao) {
    fun getVitalSignsForPatient(patientId: Int): Flow<List<VitalSign>> =
        vitalSignDao.getVitalSignsForPatient(patientId)

    suspend fun insert(vitalSign: VitalSign) {
        vitalSignDao.insertVitalSign(vitalSign)
    }
}

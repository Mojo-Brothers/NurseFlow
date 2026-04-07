package com.ivoryapp.nurseflow.ui.patient

import androidx.lifecycle.*
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.data.repository.PatientRepository
import com.ivoryapp.nurseflow.data.repository.VitalSignRepository
import kotlinx.coroutines.launch

class VitalSignViewModel(
    private val vitalSignRepository: VitalSignRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    fun getPatient(patientId: Int): LiveData<Patient?> {
        return patientRepository.getPatientById(patientId).asLiveData()
    }

    fun getVitalSigns(patientId: Int): LiveData<List<VitalSign>> {
        return vitalSignRepository.getVitalSignsForPatient(patientId).asLiveData()
    }

    fun addVitalSign(
        patientId: Int,
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        temperature: Double,
        respiration: Int,
        spo2: Int?
    ) {
        viewModelScope.launch {
            val newVital = VitalSign(
                patientId = patientId,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                temperature = temperature,
                respiration = respiration,
                spo2 = spo2
            )
            vitalSignRepository.insert(newVital)
        }
    }

    fun updateVitalSign(vitalSign: VitalSign) {
        viewModelScope.launch {
            vitalSignRepository.update(vitalSign)
        }
    }

    fun deleteVitalSign(vitalSign: VitalSign) {
        viewModelScope.launch {
            vitalSignRepository.delete(vitalSign)
        }
    }
}

class VitalSignViewModelFactory(
    private val vitalSignRepository: VitalSignRepository,
    private val patientRepository: PatientRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VitalSignViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VitalSignViewModel(vitalSignRepository, patientRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.ivoryapp.nurseflow.ui.patient

import androidx.lifecycle.*
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.data.repository.VitalSignRepository
import kotlinx.coroutines.launch

class VitalSignViewModel(private val repository: VitalSignRepository) : ViewModel() {

    fun getVitalSigns(patientId: Int): LiveData<List<VitalSign>> {
        return repository.getVitalSignsForPatient(patientId).asLiveData()
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
            repository.insert(newVital)
        }
    }
}

class VitalSignViewModelFactory(private val repository: VitalSignRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VitalSignViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VitalSignViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

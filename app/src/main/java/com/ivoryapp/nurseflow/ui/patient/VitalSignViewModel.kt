package com.ivoryapp.nurseflow.ui.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.data.model.VitalSign
import com.ivoryapp.nurseflow.data.repository.PatientRepository
import com.ivoryapp.nurseflow.data.repository.VitalSignRepository
import kotlinx.coroutines.launch

class VitalSignViewModel(
    private val vitalSignRepository: VitalSignRepository,
    private val patientRepository: PatientRepository
) : ViewModel() {

    fun getPatient(id: Int): LiveData<Patient?> = patientRepository.getPatientById(id).asLiveData()

    fun getVitalSigns(patientId: Int): LiveData<List<VitalSign>> =
        vitalSignRepository.getVitalSignsForPatient(patientId).asLiveData()

    private val _colleagueVitalSigns = MutableLiveData<List<VitalSign>>()
    val colleagueVitalSigns: LiveData<List<VitalSign>> = _colleagueVitalSigns

    fun addVitalSign(
        patientId: Int,
        systolic: Int,
        diastolic: Int,
        pulse: Int,
        temp: Double,
        resp: Int,
        spo2: Int? = null
    ) {
        viewModelScope.launch {
            val vital = VitalSign(
                patientId = patientId,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                temperature = temp,
                respiration = resp,
                spo2 = spo2
            )
            vitalSignRepository.insert(vital)
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

    fun loadColleagueVitalSigns(patientId: Int) {
        viewModelScope.launch {
            val vitals = vitalSignRepository.getColleagueVitalSigns(patientId)
            _colleagueVitalSigns.value = vitals
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

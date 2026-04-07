package com.ivoryapp.nurseflow.ui.patient

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.data.repository.PatientRepository
import kotlinx.coroutines.launch

class PatientViewModel(private val repository: PatientRepository) : ViewModel() {

    val allPatients: LiveData<List<Patient>> = repository.allPatients.asLiveData()

    fun addPatient(name: String, age: Int, dob: String, roomNumber: String, condition: String) {
        viewModelScope.launch {
            repository.insert(Patient(name = name, age = age, dateOfBirth = dob, roomNumber = roomNumber, conditionBrief = condition))
        }
    }
}

class PatientViewModelFactory(private val repository: PatientRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PatientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PatientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

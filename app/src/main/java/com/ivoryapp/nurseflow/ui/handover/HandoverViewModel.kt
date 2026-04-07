package com.ivoryapp.nurseflow.ui.handover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ivoryapp.nurseflow.data.model.Handover
import com.ivoryapp.nurseflow.data.model.HandoverTask
import com.ivoryapp.nurseflow.data.repository.HandoverRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HandoverViewModel(private val repository: HandoverRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Daftar handover aktif (yang dikirim atau diterima)
    val activeHandovers: LiveData<List<Handover>> = repository.getActiveHandovers().asLiveData()

    private val _selectedHandoverId = MutableStateFlow<String?>(null)
    
    // Checklist tugas untuk handover yang sedang dipilih
    val currentTasks: LiveData<List<HandoverTask>> = _selectedHandoverId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList())
        else repository.getHandoverTasks(id)
    }.asLiveData()

    fun getTasksForHandover(handoverId: String): LiveData<List<HandoverTask>> {
        return repository.getHandoverTasks(handoverId).asLiveData()
    }

    fun acceptHandover(handoverId: String, patientId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.acceptHandoverRequest(handoverId, patientId)
            } catch (e: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectHandover(handoverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.rejectHandoverRequest(handoverId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleTask(taskId: String, isCompleted: Boolean, handoverId: String) {
        viewModelScope.launch {
            repository.toggleTask(taskId, isCompleted, handoverId)
        }
    }

    fun sendTaskNotification(task: HandoverTask, toUid: String) {
        viewModelScope.launch {
            // Logika pengiriman notifikasi manual ke pembuat task (Perawat A)
            repository.toggleTask(task.id, task.isCompleted, task.handoverId)
        }
    }

    fun completeHandover(handoverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.completeHandover(handoverId)
                if (_selectedHandoverId.value == handoverId) {
                    _selectedHandoverId.value = null
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class HandoverViewModelFactory(private val repository: HandoverRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HandoverViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HandoverViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

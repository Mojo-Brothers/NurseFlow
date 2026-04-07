package com.ivoryapp.nurseflow.ui.handover

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ivoryapp.nurseflow.data.model.PatientHandover
import com.ivoryapp.nurseflow.data.model.ShiftSession
import com.ivoryapp.nurseflow.data.repository.HandoverRepository
import kotlinx.coroutines.launch

class HandoverViewModel(private val repository: HandoverRepository) : ViewModel() {

    private val _currentSession = MutableLiveData<ShiftSession?>()
    val currentSession: LiveData<ShiftSession?> = _currentSession

    private val _handoverItems = MutableLiveData<List<PatientHandover>>()
    val handoverItems: LiveData<List<PatientHandover>> = _handoverItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        checkActiveSession()
    }

    private fun checkActiveSession() {
        viewModelScope.launch {
            _isLoading.value = true
            val session = repository.getActiveSession()
            _currentSession.value = session
            if (session != null) {
                loadHandoverItems(session.id)
            }
            _isLoading.value = false
        }
    }

    fun startHandover(shiftType: String, fromColleagueUid: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.startNewSession(shiftType, fromColleagueUid)
            checkActiveSession()
        }
    }

    private fun loadHandoverItems(sessionId: String) {
        viewModelScope.launch {
            val items = repository.getHandoverItems(sessionId)
            _handoverItems.value = items
        }
    }

    fun updateHandoverItem(item: PatientHandover) {
        viewModelScope.launch {
            repository.updateHandoverItem(item)
            _currentSession.value?.id?.let { loadHandoverItems(it) }
        }
    }

    fun completeHandover() {
        viewModelScope.launch {
            _isLoading.value = true
            _currentSession.value?.id?.let { 
                repository.completeSession(it)
                _currentSession.value = null
                _handoverItems.value = emptyList()
            }
            _isLoading.value = false
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

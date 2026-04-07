package com.ivoryapp.nurseflow.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ivoryapp.nurseflow.data.repository.TaskRepository

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {
    // ViewModel logic for HomeFragment (Patient status, colleagues, etc.) can be added here
}

class HomeViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

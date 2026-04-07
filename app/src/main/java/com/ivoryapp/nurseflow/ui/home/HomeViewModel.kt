package com.ivoryapp.nurseflow.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ivoryapp.nurseflow.data.model.Task
import com.ivoryapp.nurseflow.data.repository.TaskRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TaskRepository) : ViewModel() {

    val allTasks: LiveData<List<Task>> = repository.allTasks.asLiveData()

    fun updateTask(task: Task, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = isCompleted))
        }
    }

    fun addTask(title: String, description: String) {
        viewModelScope.launch {
            repository.insert(Task(title = title, description = description))
        }
    }
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

package com.ivoryapp.nurseflow.data.repository

import com.ivoryapp.nurseflow.data.local.TaskDao
import com.ivoryapp.nurseflow.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }
}

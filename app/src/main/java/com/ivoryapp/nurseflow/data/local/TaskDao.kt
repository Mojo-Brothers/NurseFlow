package com.ivoryapp.nurseflow.data.local

import androidx.room.*
import com.ivoryapp.nurseflow.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Delete
    suspend fun deleteTask(task: Task): Int

    @Update
    suspend fun updateTask(task: Task): Int
}

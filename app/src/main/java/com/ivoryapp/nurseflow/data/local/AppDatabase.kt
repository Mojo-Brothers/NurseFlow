package com.ivoryapp.nurseflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ivoryapp.nurseflow.data.model.NursingNote
import com.ivoryapp.nurseflow.data.model.Patient
import com.ivoryapp.nurseflow.data.model.Task
import com.ivoryapp.nurseflow.data.model.VitalSign

@Database(
    entities = [Task::class, Patient::class, VitalSign::class, NursingNote::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun patientDao(): PatientDao
    abstract fun vitalSignDao(): VitalSignDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nurseflow_database"
                )
                .fallbackToDestructiveMigration() // For development only
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

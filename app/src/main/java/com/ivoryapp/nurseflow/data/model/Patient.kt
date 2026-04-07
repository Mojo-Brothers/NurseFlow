package com.ivoryapp.nurseflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val dateOfBirth: String = "",
    val roomNumber: String,
    val conditionBrief: String,
    val createdAt: Long = System.currentTimeMillis()
)

package com.ivoryapp.nurseflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vital_signs")
data class VitalSign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val temperature: Double,
    val respiration: Int,
    val spo2: Int? = null,
    val painScale: Int? = null,
    val consciousness: String = "Alert", // Alert, Voice, Pain, Unresponsive (AVPU)
    val timestamp: Long = System.currentTimeMillis()
)

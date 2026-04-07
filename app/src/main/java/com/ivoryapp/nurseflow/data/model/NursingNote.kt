package com.ivoryapp.nurseflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nursing_notes")
data class NursingNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: Int,
    val content: String,
    val soapFormat: String? = null, // Store generated AI SOAP
    val isAiGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

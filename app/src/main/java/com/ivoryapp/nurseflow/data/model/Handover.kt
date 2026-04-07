package com.ivoryapp.nurseflow.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class HandoverPriority {
    LOW, MEDIUM, HIGH
}

data class Handover(
    val id: String = "",
    val patientId: Int = 0,
    val patientName: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val status: String = "PENDING", // PENDING, ACCEPTED, REJECTED, COMPLETED
    val notes: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val acceptedAt: Timestamp? = null
)

data class HandoverTask(
    val id: String = "",
    val handoverId: String = "",
    val title: String = "",
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val createdBy: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class ShiftSession(
    val id: String = "",
    val shiftType: String = "",
    val fromUid: String = "",
    val createdBy: String = "",
    val status: String = "ACTIVE", // ACTIVE, COMPLETED
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null
)

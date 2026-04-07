package com.ivoryapp.nurseflow.data.model

import com.google.firebase.Timestamp
import java.util.UUID

data class ShiftSession(
    val id: String = "",
    val shiftType: String = "", // Pagi, Siang, Malam
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp? = null,
    val createdBy: String = "",
    val fromUid: String = "", // UID perawat pemberi operan
    val teamMembers: List<String> = emptyList(),
    val status: String = "ACTIVE" // ACTIVE, COMPLETED
)

data class PatientHandover(
    val id: String = "",
    val sessionId: String = "",
    val patientId: Int = 0,
    val patientName: String = "",
    val roomNumber: String = "",
    val summary: String = "",
    val latestNews2Score: Int = 0,
    val priority: HandoverPriority = HandoverPriority.LOW,
    val tasks: List<HandoverInstruction> = emptyList(),
    val notes: String = "",
    val isFlagged: Boolean = false,
    val isDiscussed: Boolean = false,
    val lastVitalsSummary: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class HandoverInstruction(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val isCompleted: Boolean = false
)

enum class HandoverPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

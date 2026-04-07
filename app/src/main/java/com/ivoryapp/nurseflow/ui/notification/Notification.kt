package com.ivoryapp.nurseflow.ui.notification

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Timestamp? = null,
    val fromUid: String? = null,
    val patientId: Int? = null, // Link to specific patient for clinical alerts
    var aiSummary: String? = null, // Ringkasan AI yang dikirim rekan
    val status: String? = null, 
    val isRead: Boolean = false
)

enum class NotificationType {
    SYSTEM,
    APP,
    FRIEND_REQUEST,
    CLINICAL_ALERT
}

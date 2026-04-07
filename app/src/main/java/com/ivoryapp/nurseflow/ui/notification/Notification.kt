package com.ivoryapp.nurseflow.ui.notification

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Notification(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val timestamp: Timestamp? = null,
    val fromUid: String? = null,
    val fromName: String? = null,
    val handoverId: String? = null,
    val patientId: Int? = null,
    val taskCount: Int? = null,
    var aiSummary: String? = null,
    val status: String? = "pending", // pending, accepted, rejected
    
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false
)

enum class NotificationType {
    SYSTEM,
    APP,
    FRIEND_REQUEST,
    HANDOVER_REQUEST,
    CLINICAL_ALERT
}

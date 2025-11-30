package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    val id: String = "",
    val userId: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val message: String = "",
    val postId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val requiresAction: Boolean = false, // For willing notifications that need accept/reject
    val actionTaken: Boolean = false // Track if user has accepted/rejected
)

enum class NotificationType {
    LIKE,
    COMMENT,
    REPLY,
    FRIEND_REQUEST,
    MESSAGE,
    WILLING // Add this new type
}

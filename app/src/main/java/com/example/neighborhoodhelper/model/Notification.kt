package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Notification(
    @DocumentId
    val id: String = "",
    val userId: String = "", // Recipient user ID
    val fromUserId: String = "", // Who triggered the notification
    val fromUsername: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val postId: String = "",
    val commentId: String? = null,
    val message: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

enum class NotificationType {
    LIKE,
    COMMENT,
    REPLY
}

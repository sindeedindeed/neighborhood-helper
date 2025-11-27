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
    val type: String = "LIKE", // Changed to String for Firebase compatibility
    val postId: String = "",
    val commentId: String? = null,
    val message: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

object NotificationType {
    const val LIKE = "LIKE"
    const val COMMENT = "COMMENT"
    const val REPLY = "REPLY"
    const val FRIEND_REQUEST = "FRIEND_REQUEST"
    const val MESSAGE = "MESSAGE"
}
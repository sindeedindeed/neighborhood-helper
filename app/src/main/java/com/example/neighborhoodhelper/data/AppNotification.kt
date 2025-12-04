package com.example.neighborhoodhelper.data

import com.google.firebase.Timestamp

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val type: String = "", // COMMENT, WILLING, REQUEST_ACCEPTED, REQUEST_REJECTED, LIKE
    val message: String = "",
    val postId: String? = null,
    val createdAt: Timestamp? = null,
    val isRead: Boolean = false,
    val title: String = "",
    val requiresAction: Boolean = false,
    val actionData: Map<String, String>? = null,
    val isViewed: Boolean = false, // Track if notification has been viewed
    val wasRejected: Boolean = false // Track if willing request was rejected
) {
    fun getFormattedTime(): String {
        if (createdAt == null) return "Just now"

        val now = System.currentTimeMillis()
        val diff = now - createdAt.toDate().time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
}
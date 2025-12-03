package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class UserRating(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val toUserId: String = "",
    val toUsername: String = "",
    val rating: Float = 0f, // 1-5 stars
    val review: String = "",
    val category: String = "OTHER",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)


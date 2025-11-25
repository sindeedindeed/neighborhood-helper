package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val timestamp: String = "" // Human-readable time
)

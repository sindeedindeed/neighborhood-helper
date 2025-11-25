package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userAvatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val location: String? = null,
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(), // User IDs who liked
    val comments: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val timestamp: String = "" // Human-readable time (e.g., "5m", "2h")
)

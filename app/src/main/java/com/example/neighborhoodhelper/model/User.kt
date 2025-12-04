package com.example.neighborhoodhelper.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val fcmToken: String? = null,
    val friends: List<String> = emptyList(), // List of friend user IDs
    val friendRequests: List<String> = emptyList(), // Pending friend request IDs
    val pendingRequests: List<String> = emptyList(), // Outgoing friend request IDs
    val averageRating: Float = 0f, // 0-5 stars
    val totalRatings: Int = 0,
    val tasksCompleted: Int = 0,
    val tasksHelped: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lastKnownLatitude: Double? = null,
    val lastKnownLongitude: Double? = null
)
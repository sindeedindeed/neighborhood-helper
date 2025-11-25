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
    val friendRequests: List<String> = emptyList() // Pending friend request IDs
)

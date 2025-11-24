package com.example.neighborhoodhelper.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val fcmToken: String? = null // For push notifications
)
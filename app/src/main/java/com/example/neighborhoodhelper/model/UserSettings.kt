package com.example.neighborhoodhelper.model

import com.google.firebase.firestore.DocumentId

data class UserSettings(
    @DocumentId
    val userId: String = "",
    val notificationsEnabled: Boolean = true,
    val friendRequestNotifications: Boolean = true,
    val messageNotifications: Boolean = true,
    val postNotifications: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)
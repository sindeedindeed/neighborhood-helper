package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val location: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val timestamp: String = "",
    val willingUsers: List<String> = emptyList(),
    val status: String = "active", // active, matched, completed, cancelled
    val willingUserDetails: List<WillingUser> = emptyList()
)

data class WillingUser(
    val userId: String = "",
    val userName: String = "",
    val userProfileUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val status: String = "pending" // pending, accepted, rejected
)

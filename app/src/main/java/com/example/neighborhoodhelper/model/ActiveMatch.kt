package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ActiveMatch(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val requesterPhone: String = "",
    val requesterLat: Double = 0.0,
    val requesterLon: Double = 0.0,
    val helperId: String = "",
    val helperName: String = "",
    val helperPhone: String = "",
    val helperLat: Double = 0.0,
    val helperLon: Double = 0.0,
    val status: String = "active", // active, arrived, completed
    val distance: Float = 0f, // in meters
    val isProximityReached: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastUpdated: Timestamp? = null
)


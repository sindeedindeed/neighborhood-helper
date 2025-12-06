package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class TaskHistory(
    @DocumentId
    val id: String = "",
    val postId: String = "",
    val postContent: String = "",
    val category: String = "OTHER",
    val requesterId: String = "",
    val requesterName: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val status: String = "completed", // completed, cancelled
    val requesterRating: Float? = null,
    val helperRating: Float? = null,
    val requesterReview: String? = null,
    val helperReview: String? = null,
    @ServerTimestamp
    val completedAt: Timestamp? = null,
    val matchedAt: Timestamp? = null
)


package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class FriendRequest(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromAvatarUrl: String = "",
    val toUserId: String = "",
    val toUsername: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
package com.example.neighborhoodhelper.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ChatRoom(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(), // User IDs
    val participantNames: Map<String, String> = emptyMap(), // userId -> username
    val participantAvatars: Map<String, String> = emptyMap(), // userId -> avatarUrl
    val lastMessage: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
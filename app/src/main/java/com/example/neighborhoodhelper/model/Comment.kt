package com.example.neighborhoodhelper.model

data class Comment(
    val id: String,
    val postId: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val timestamp: String
)
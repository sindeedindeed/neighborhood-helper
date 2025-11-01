package com.example.neighborhoodhelper.model

/** Simple Comment model for posts */
data class Comment(
    val id: String,
    val postId: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val timestamp: String
)


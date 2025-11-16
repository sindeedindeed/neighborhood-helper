package com.example.neighborhoodhelper.model

data class Post(
    val id: String,
    val username: String,
    val userAvatarUrl: String,
    val timestamp: String,
    val content: String,
    val imageUrl: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val location: String? = null
)
package com.example.neighborhoodhelper.model

/**
 * Post data model representing a neighbor's post in the feed.
 */
data class Post(
    val id: String,
    val username: String,
    val userAvatarUrl: String,
    val timestamp: String,
    val content: String,
    val imageUrl: String?,
    val likes: Int,
    val comments: Int
)

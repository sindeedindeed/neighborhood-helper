package com.example.neighborhoodhelper.ui.feed

import androidx.lifecycle.ViewModel
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FeedViewModel : ViewModel() {
    private val _posts = MutableStateFlow(samplePosts())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _comments = MutableStateFlow(sampleComments())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    fun accept(postId: String) {
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(likes = it.likes + 1) else it }
        }
    }

    fun addComment(postId: String, author: String, text: String) {
        if (text.isBlank()) return
        val id = UUID.randomUUID().toString()
        val timestamp = "Just now"
        val comment = Comment(
            id = id,
            postId = postId,
            author = author,
            authorAvatarUrl = "",
            text = text,
            timestamp = timestamp
        )
        _comments.update { list -> list + comment }
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(comments = it.comments + 1) else it }
        }
    }

    companion object {
        private fun samplePosts(): List<Post> = listOf(
            Post(
                id = "1",
                username = "Maishan Nadis",
                userAvatarUrl = "",
                timestamp = "5m",
                content = "Lost cat in Kolabagan. White with grey spots, answers to Mimi. Please contact if seen!",
                imageUrl = "https://images.unsplash.com/photo-1574158622682-e40e69881006?w=400",
                likes = 10,
                comments = 2,
                location = "Kolabagan"
            ),
            Post(
                id = "2",
                username = "Mehedi Srabon",
                userAvatarUrl = "",
                timestamp = "2h",
                content = "Looking for someone to help with moving furniture tomorrow.",
                imageUrl = null,
                likes = 7,
                comments = 3,
                location = "Mirpur"
            ),
            Post(
                id = "3",
                username = "Safwat Bushra",
                userAvatarUrl = "",
                timestamp = "10m",
                content = "Anyone has charger? Type C needed urgently.",
                imageUrl = "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=400",
                likes = 4,
                comments = 1,
                location = "Dhanmondi"
            ),
            Post(
                id = "4",
                username = "Shafi Alam",
                userAvatarUrl = "",
                timestamp = "1h",
                content = "Offering extra groceries to share. Have some vegetables and fruits.",
                imageUrl = null,
                likes = 3,
                comments = 5,
                location = "Banani"
            )
        )

        private fun sampleComments(): List<Comment> = listOf(
            Comment(
                id = UUID.randomUUID().toString(),
                postId = "1",
                author = "Ayesha Khan",
                authorAvatarUrl = "",
                text = "I can help look for the cat near the market.",
                timestamp = "5m"
            ),
            Comment(
                id = UUID.randomUUID().toString(),
                postId = "1",
                author = "Rafi Ahmed",
                authorAvatarUrl = "",
                text = "I saw a similar cat yesterday near the park.",
                timestamp = "10m"
            ),
            Comment(
                id = UUID.randomUUID().toString(),
                postId = "2",
                author = "Nadia Rahman",
                authorAvatarUrl = "",
                text = "I have a Type C charger you can borrow!",
                timestamp = "8m"
            )
        )
    }
}
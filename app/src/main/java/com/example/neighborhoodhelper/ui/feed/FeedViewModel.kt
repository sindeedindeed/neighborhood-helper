package com.example.neighborhoodhelper.ui.feed

import androidx.lifecycle.ViewModel
import com.example.neighborhoodhelper.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class FeedViewModel : ViewModel() {

    private val _posts = MutableStateFlow(generateDummyPosts())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    fun like(postId: String) {
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(likes = it.likes + 1) else it }
        }
    }

    fun comment(postId: String) {
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(comments = it.comments + 1) else it }
        }
    }

    companion object {
        private fun generateDummyPosts(): List<Post> = listOf(
            Post(
                id = UUID.randomUUID().toString(),
                username = "Alex Johnson",
                userAvatarUrl = "",
                timestamp = "2m",
                content = "Lost cat near Maple St. Please keep an eye out!",
                imageUrl = null,
                likes = 4,
                comments = 2
            ),
            Post(
                id = UUID.randomUUID().toString(),
                username = "Priya Singh",
                userAvatarUrl = "",
                timestamp = "15m",
                content = "Anyone has a ladder I can borrow this afternoon?",
                imageUrl = null,
                likes = 1,
                comments = 5
            ),
            Post(
                id = UUID.randomUUID().toString(),
                username = "Miguel Santos",
                userAvatarUrl = "",
                timestamp = "1h",
                content = "Community garden meetup was a success! Here's a photo of today's harvest.",
                imageUrl = "https://example.com/demo-image.jpg",
                likes = 12,
                comments = 3
            )
        )
    }
}

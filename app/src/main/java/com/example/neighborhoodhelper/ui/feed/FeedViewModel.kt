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

    // Accept action (uses likes as the accepted counter for now)
    fun accept(postId: String) {
        _posts.update { list ->
            list.map { if (it.id == postId) it.copy(likes = it.likes + 1) else it }
        }
    }

    companion object {
        private fun generateDummyPosts(): List<Post> = listOf(
            Post(
                id = UUID.randomUUID().toString(),
                username = "Maishan Nadis",
                userAvatarUrl = "",
                timestamp = "2m",
                content = "Lost cat near Kalabagan Please keep an eye out!",
                imageUrl = null,
                likes = 4,
                comments = 2
            ),
            Post(
                id = UUID.randomUUID().toString(),
                username = "Faiza Tashmeah",
                userAvatarUrl = "",
                timestamp = "15m",
                content = "Anyone has a charger-fan I can borrow this afternoon?",
                imageUrl = null,
                likes = 1,
                comments = 5
            ),
            Post(
                id = UUID.randomUUID().toString(),
                username = "Safwat Bushra",
                userAvatarUrl = "",
                timestamp = "1h",
                content = "Need a Math tutor for my cousin. Any recommendations?",
                imageUrl = null,
                likes = 12,
                comments = 3
            )
        )
    }
}

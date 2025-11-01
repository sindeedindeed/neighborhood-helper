package com.example.neighborhoodhelper.ui.feed

import androidx.lifecycle.ViewModel
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
        val timestamp = nowShort()
        val comment = Comment(id = id, postId = postId, author = author, authorAvatarUrl = "", text = text, timestamp = timestamp)
        _comments.update { list -> list + comment }
        _posts.update { list -> list.map { if (it.id == postId) it.copy(comments = it.comments + 1) else it } }
    }

    private fun nowShort(): String = try {
        val formatter = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
        formatter.format(Instant.now())
    } catch (e: Exception) {
        "now"
    }

    companion object {
        private fun samplePosts(): List<Post> = listOf(
            Post(
                id = "1",
                username = "Maishan Nadis",
                userAvatarUrl = "",
                timestamp = "Oct 10",
                content = "Maishan lost his cat in Kolabagan. Please keep an eye out!",
                imageUrl = "",
                likes = 2,
                comments = 3,
                location = "Kolabagan"
            ),
            Post(
                id = "2",
                username = "Faiza Tashmeah",
                userAvatarUrl = "",
                timestamp = "Oct 9",
                content = "Anyone has a charger-fan I can borrow this afternoon?",
                imageUrl = null,
                likes = 1,
                comments = 1,
                location = "Dhanmondi"
            ),
            Post(
                id = "3",
                username = "Safwat Bushra",
                userAvatarUrl = "",
                timestamp = "Oct 8",
                content = "Need a Math tutor for my cousin. Any recommendations?",
                imageUrl = null,
                likes = 0,
                comments = 0,
                location = "Banani"
            )
        )

        private fun sampleComments(): List<Comment> = listOf(
            Comment(id = UUID.randomUUID().toString(), postId = "1", author = "Ayesha", authorAvatarUrl = "", text = "I can help look for the cat near the market.", timestamp = "5m"),
            Comment(id = UUID.randomUUID().toString(), postId = "1", author = "Rafi", authorAvatarUrl = "", text = "I saw a similar cat yesterday.", timestamp = "10m")
        )
    }
}

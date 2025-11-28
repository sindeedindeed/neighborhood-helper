package com.example.neighborhoodhelper.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import com.example.neighborhoodhelper.model.Notification
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.example.neighborhoodhelper.utils.ImageUploadManager

class FeedViewModel : ViewModel() {
    var showLogoutDialog: Boolean
        get() {
            TODO()
        }
        set(value) {}
    private val repository = FirebaseRepository()

    // Posts
    val posts: StateFlow<List<Post>> = repository.observePosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get comments for specific post - THIS IS THE CORRECT WAY
    fun getCommentsForPost(postId: String): StateFlow<List<Comment>> {
        return repository.observeComments(postId)
            .catch { e ->
                Log.w("FeedViewModel", "Error loading comments for post $postId", e)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    // Notifications
    val notifications: StateFlow<List<Notification>> = repository.observeNotifications()
        .catch { e ->
            Log.w("FeedViewModel", "Error loading notifications", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Unread notification count
    val unreadNotificationCount: StateFlow<Int> = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Create a post
    fun createPost(content: String, imageUrl: String? = null, location: String? = null) {
        if (content.isBlank()) {
            _uiState.value = UiState.Error("Post content cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createPost(content, imageUrl, location)
            _uiState.value = if (result.isSuccess) {
                UiState.Success("Post created successfully")
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
            }
        }
    }
    private val imageUploadManager = ImageUploadManager()

    fun createPostWithImage(content: String, imageUri: Uri?, location: String?, context: Context) {
        if (content.isBlank()) {
            _uiState.value = UiState.Error("Post content cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            // Upload image if selected
            val imageUrl = if (imageUri != null) {
                val uploadResult = imageUploadManager.uploadPostImage(imageUri, context)
                if (uploadResult.isFailure) {
                    _uiState.value = UiState.Error("Failed to upload image")
                    return@launch
                }
                uploadResult.getOrNull()
            } else {
                null
            }

            // Create post with image URL
            val result = repository.createPost(content, imageUrl, location)
            _uiState.value = if (result.isSuccess) {
                UiState.Success("Post created successfully")
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
            }
        }
    }

    // Toggle like on a post
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

    // Legacy method name for compatibility
    fun accept(postId: String) = toggleLike(postId)

    // Add a comment
    fun addComment(postId: String, author: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val result = repository.addComment(postId, text)
            if (result.isFailure) {
                _uiState.value = UiState.Error("Failed to add comment")
            }
        }
    }
    fun refreshFeed() {
        viewModelScope.launch {
            try {
                Log.d("FeedViewModel", "Feed refresh triggered - Firestore will update automatically")
                // The StateFlow automatically refreshes since it's observing Firestore in real-time
                // Force a UI update by collecting once
                posts.take(1).collect {
                    Log.d("FeedViewModel", "Current posts count: ${it.size}")
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error during refresh", e)
            }
        }
    }

    // Mark notification as read
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    // Reset UI state
    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}


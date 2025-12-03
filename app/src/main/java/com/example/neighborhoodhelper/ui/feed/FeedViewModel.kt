package com.example.neighborhoodhelper.ui.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import com.example.neighborhoodhelper.data.AppNotification
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import android.net.Uri
import com.example.neighborhoodhelper.utils.ImageUploadManager
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue
import kotlin.text.get

class FeedViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val imageUploadManager = ImageUploadManager()

    var showLogoutDialog: Boolean = false

    // Posts
    val posts: StateFlow<List<Post>> = repository.observePosts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Get comments for specific post
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
    val notifications: StateFlow<List<AppNotification>> = repository.observeNotifications()
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
    fun createPost(content: String, imageUrl: String? = null, location: String? = null, category: String = "OTHER") {
        if (content.isBlank()) {
            _uiState.value = UiState.Error("Post content cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createPost(content, imageUrl, location, category)
            _uiState.value = if (result.isSuccess) {
                UiState.Success("Post created successfully")
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
            }
        }
    }

    fun createPostWithImage(content: String, imageUri: Uri?, location: String?, category: String = "OTHER", context: Context) {
        if (content.isBlank()) {
            _uiState.value = UiState.Error("Post content cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val imageUrl = if (imageUri != null) {
                    Log.d("FeedViewModel", "Starting image upload...")
                    val uploadResult = imageUploadManager.uploadPostImage(imageUri, context)

                    if (uploadResult.isFailure) {
                        val error = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                        Log.e("FeedViewModel", "Image upload failed: $error")
                        _uiState.value = UiState.Error("Failed to upload image: $error")
                        return@launch
                    }

                    val url = uploadResult.getOrNull()
                    Log.d("FeedViewModel", "Image uploaded successfully: $url")
                    url
                } else {
                    null
                }

                val result = repository.createPost(content, imageUrl, location, category)
                _uiState.value = if (result.isSuccess) {
                    UiState.Success("Post created successfully")
                } else {
                    UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
                }
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error creating post", e)
                _uiState.value = UiState.Error("Error: ${e.message}")
            }
        }
    }


    // Toggle like - REMOVED to avoid conflict with new accept()
    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId)
        }
    }

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
                Log.d("FeedViewModel", "Feed refresh triggered")
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

    fun acceptWillingRequest(notificationId: String, postId: String, willingUserId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch

                db.collection("notifications")
                    .document(notificationId)
                    .update(mapOf("actionTaken" to true))
                    .await()

                val acceptNotification = hashMapOf(
                    "userId" to willingUserId,
                    "type" to "WILLING_ACCEPTED",
                    "message" to "Your willing request was accepted!",
                    "postId" to postId,
                    "fromUserId" to currentUserId,
                    "fromUsername" to (auth.currentUser?.displayName ?: "User"),
                    "timestamp" to System.currentTimeMillis(),
                    "isRead" to false,
                    "requiresAction" to false,
                    "actionTaken" to false
                )

                db.collection("notifications").add(acceptNotification).await()

            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to accept willing request")
            }
        }
    }

    fun rejectWillingRequest(notificationId: String) {
        viewModelScope.launch {
            try {
                db.collection("notifications")
                    .document(notificationId)
                    .update(mapOf("actionTaken" to true))
                    .await()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to reject willing request")
            }
        }
    }

    // NEW accept function - handles willing with notifications
    fun accept(postId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch
                val currentUsername = auth.currentUser?.displayName ?: "Anonymous"

                val postSnapshot = db.collection("posts").document(postId).get().await()
                val postOwnerId = postSnapshot.getString("userId") ?: return@launch

                // Don't allow liking own post
                if (postOwnerId == currentUserId) return@launch

                val willingUsers = postSnapshot.get("willingUsers") as? List<String> ?: emptyList()
                val isCurrentlyWilling = willingUsers.contains(currentUserId)

                if (isCurrentlyWilling) {
                    // Remove willing
                    db.collection("posts")
                        .document(postId)
                        .update(
                            mapOf(
                                "willingUsers" to FieldValue.arrayRemove(currentUserId),
                                "likes" to (postSnapshot.getLong("likes") ?: 1) - 1
                            )
                        )
                        .await()
                } else {
                    // Add willing
                    db.collection("posts")
                        .document(postId)
                        .update(
                            mapOf(
                                "willingUsers" to FieldValue.arrayUnion(currentUserId),
                                "likes" to (postSnapshot.getLong("likes") ?: 0) + 1
                            )
                        )
                        .await()

                    // Create notification only when marking as willing
                    val notification = hashMapOf(
                        "userId" to postOwnerId,
                        "type" to "WILLING",
                        "message" to "$currentUsername is willing to help with your post",
                        "postId" to postId,
                        "fromUserId" to currentUserId,
                        "fromUsername" to currentUsername,
                        "timestamp" to System.currentTimeMillis(),
                        "isRead" to false,
                        "requiresAction" to true,
                        "actionTaken" to false
                    )

                    db.collection("notifications").add(notification).await()
                }

                refreshFeed()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to mark as willing")
            }
        }
    }


    fun navigateToPostFromNotification(postId: String, notificationId: String, navController: NavController) {
        viewModelScope.launch {
            markNotificationAsRead(notificationId)
            navController.navigate("postDetail/$postId")
        }
    }
    fun markAllNotificationsAsViewed() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid ?: return@launch

                val unreadNotifications = db.collection("notifications")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()

                val batch = db.batch()
                for (doc in unreadNotifications.documents) {
                    batch.update(doc.reference, "isRead", true)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Error marking notifications as viewed", e)
            }
        }
    }


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

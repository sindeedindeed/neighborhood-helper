package com.example.neighborhoodhelper.ui.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.FriendRequest
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Friend requests
    val friendRequests: StateFlow<List<FriendRequest>> = repository.observeFriendRequests()
        .catch { e ->
            Log.w("FriendsViewModel", "Error loading friend requests", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Friends list
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    // Friends' posts
    val friendsPosts: StateFlow<List<Post>> = repository.observeFriendsPosts()
        .catch { e ->
            Log.w("FriendsViewModel", "Error loading friends posts", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadFriends()
    }

    fun loadFriends() {
        viewModelScope.launch {
            val result = repository.getFriends()
            if (result.isSuccess) {
                _friends.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.searchUsers(query)
            if (result.isSuccess) {
                _searchResults.value = result.getOrNull() ?: emptyList()
                _uiState.value = UiState.Success("")
            } else {
                _uiState.value = UiState.Error("Failed to search users")
            }
        }
    }

    fun sendFriendRequest(toUserId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.sendFriendRequest(toUserId)
            _uiState.value = if (result.isSuccess) {
                UiState.Success("Friend request sent")
            } else {
                UiState.Error("Failed to send request")
            }
        }
    }

    fun acceptFriendRequest(requestId: String, fromUserId: String) {
        viewModelScope.launch {
            val result = repository.acceptFriendRequest(requestId, fromUserId)
            if (result.isSuccess) {
                loadFriends()
            }
        }
    }

    fun rejectFriendRequest(requestId: String, fromUserId: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(requestId, fromUserId)
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
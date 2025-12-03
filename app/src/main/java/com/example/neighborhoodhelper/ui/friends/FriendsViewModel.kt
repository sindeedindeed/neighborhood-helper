package com.example.neighborhoodhelper.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.User
import com.example.neighborhoodhelper.model.FriendRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FriendsViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Friends list
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    // Friend requests
    val friendRequests: StateFlow<List<FriendRequest>> = repository.observeFriendRequests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            repository.getFriends().onSuccess { friendsList ->
                _friends.value = friendsList
            }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            repository.searchUsers(query).onSuccess { users ->
                _searchResults.value = users
            }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            repository.sendFriendRequest(userId)
        }
    }

    fun acceptFriendRequest(requestId: String, fromUserId: String) {
        viewModelScope.launch {
            repository.acceptFriendRequest(requestId, fromUserId).onSuccess {
                loadFriends()
            }
        }
    }

    fun rejectFriendRequest(requestId: String, fromUserId: String) {
        viewModelScope.launch {
            repository.rejectFriendRequest(requestId, fromUserId)
        }
    }
}

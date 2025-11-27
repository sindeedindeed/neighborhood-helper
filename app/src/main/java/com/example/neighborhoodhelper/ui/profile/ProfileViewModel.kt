package com.example.neighborhoodhelper.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            _currentUser.value = user
        }
    }

    fun createProfile(username: String, email: String, phoneNumber: String, bio: String) {
        if (username.isBlank()) {
            _uiState.value = UiState.Error("Username is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createOrUpdateUserProfile(
                username = username.trim(),
                email = email.trim(),
                phoneNumber = phoneNumber.trim(),
                bio = bio.trim()
            )

            _uiState.value = if (result.isSuccess) {
                loadCurrentUser()
                UiState.Success("Profile created successfully")
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to create profile")
            }
        }
    }

    fun updateProfile(username: String, email: String, phoneNumber: String, bio: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.createOrUpdateUserProfile(username, email, phoneNumber, bio)
            _uiState.value = if (result.isSuccess) {
                loadCurrentUser()
                UiState.Success("Profile updated successfully")
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update profile")
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}

package com.example.neighborhoodhelper.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.UserSettings
import com.google.firebase.auth.FirebaseAuth  // ADD THIS IMPORT
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()  // ADD THIS LINE

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val result = repository.getUserSettings()
            if (result.isSuccess) {
                _settings.value = result.getOrNull()
            }
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.updateUserSettings(newSettings)
            _uiState.value = if (result.isSuccess) {
                _settings.value = newSettings
                UiState.Success("Settings updated")
            } else {
                UiState.Error("Failed to update settings")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = UiState.Idle
    }

    // ADD THIS NEW FUNCTION
    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                Log.d("SettingsViewModel", "User logged out successfully")
                _uiState.value = UiState.LoggedOut
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Logout failed", e)
                _uiState.value = UiState.Error("Logout failed: ${e.message}")
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
        object LoggedOut : UiState()  // ADD THIS NEW STATE
    }
}
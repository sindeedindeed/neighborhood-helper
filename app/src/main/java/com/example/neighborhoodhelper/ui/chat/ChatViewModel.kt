package com.example.neighborhoodhelper.ui.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.ChatRoom
import com.example.neighborhoodhelper.model.Message
import com.example.neighborhoodhelper.utils.ImageUploadManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val imageUploadManager = ImageUploadManager()

    // Chat rooms
    val chatRooms: StateFlow<List<ChatRoom>> = repository
        .runCatching { this::class.java.getDeclaredMethod("observeChatRooms").invoke(this) as Flow<List<ChatRoom>> }
        .getOrDefault(flowOf(emptyList()))
        .catch { e ->
            Log.w("ChatViewModel", "Error loading chat rooms", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current room messages
    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    val messages: StateFlow<List<Message>> = _currentRoomId
        .filterNotNull()
        .flatMapLatest { roomId ->
            repository.runCatching {
                this::class.java.getDeclaredMethod("observeMessages", String::class.java)
                    .invoke(this, roomId) as Flow<List<Message>>
            }.getOrDefault(flowOf(emptyList()))
                .catch { e ->
                    Log.w("ChatViewModel", "Error loading messages", e)
                    emit(emptyList())
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // UI State
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun openChatWithUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.runCatching {
                this::class.java.getDeclaredMethod("getOrCreateChatRoom", String::class.java)
                    .invoke(this, userId) as Result<String>
            }.getOrDefault(Result.failure(Exception("Method not found")))

            if (result.isSuccess) {
                _currentRoomId.value = result.getOrNull()
                _uiState.value = UiState.Success("")
            } else {
                _uiState.value = UiState.Error("Failed to open chat")
            }
        }
    }

    fun setCurrentRoom(roomId: String) {
        _currentRoomId.value = roomId
    }

    fun sendMessage(text: String) {
        val roomId = _currentRoomId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            val result = repository.runCatching {
                this::class.java.getDeclaredMethod("sendMessage", String::class.java, String::class.java, String::class.java)
                    .invoke(this, roomId, text, null) as Result<Unit>
            }.getOrDefault(Result.failure(Exception("Method not found")))

            if (result.isFailure) {
                _uiState.value = UiState.Error("Failed to send message")
            }
        }
    }

    fun sendMessageWithImage(text: String, imageUri: Uri, context: Context) {
        val roomId = _currentRoomId.value ?: return

        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val uploadResult = imageUploadManager.uploadPostImage(imageUri, context)
            if (uploadResult.isFailure) {
                _uiState.value = UiState.Error("Failed to upload image")
                return@launch
            }

            val imageUrl = uploadResult.getOrNull()
            val result = repository.runCatching {
                this::class.java.getDeclaredMethod("sendMessage", String::class.java, String::class.java, String::class.java)
                    .invoke(this, roomId, text, imageUrl) as Result<Unit>
            }.getOrDefault(Result.failure(Exception("Method not found")))

            _uiState.value = if (result.isSuccess) {
                UiState.Success("")
            } else {
                UiState.Error("Failed to send message")
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

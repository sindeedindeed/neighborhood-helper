package com.example.neighborhoodhelper.ui.post

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

data class PostData(
    val authorId: String = "user_123",
    val authorName: String = "John Doe",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val imageBitmap: Bitmap? = null,
    val isUrgent: Boolean = false
)

// Simple serializable data for Firestore (no Bitmap).
data class PostRecord(
    val id: String = "",
    val authorId: String = "user_123",
    val authorName: String = "John Doe",
    val text: String = "",
    val imageUrl: String? = null,
    val isUrgent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap.asStateFlow()

    private val _isUrgent = MutableStateFlow(false)
    val isUrgent: StateFlow<Boolean> = _isUrgent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val firestore = Firebase.firestore

    init {
        // Initialize Firebase with Application context if not already
        try {
            FirebaseApp.initializeApp(application)
        } catch (_: Exception) {
            // Already initialized or failed silently
        }
    }

    fun setText(value: String) {
        _text.value = value
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        _imageBitmap.value = bitmap
    }

    fun setUrgent(flag: Boolean) {
        _isUrgent.value = flag
    }

    fun clear() {
        _text.value = ""
        _imageBitmap.value = null
        _isUrgent.value = false
    }

    /**
     * Submit post: writes a PostRecord to Firestore under collection `posts`.
     * Note: Image upload is not implemented (requires Storage). We store null for imageUrl.
     */
    fun submitPost(onResult: (Result<PostRecord>) -> Unit) {
        val id = UUID.randomUUID().toString()
        val record = PostRecord(
            id = id,
            text = _text.value.trim(),
            isUrgent = _isUrgent.value
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("posts").document(id).set(record)
                    .addOnSuccessListener {
                        onResult(Result.success(record))
                    }
                    .addOnFailureListener { ex ->
                        onResult(Result.failure(ex))
                    }
            } catch (ex: Exception) {
                onResult(Result.failure(ex))
            } finally {
                _isLoading.value = false
            }
            clear()
        }
    }
}

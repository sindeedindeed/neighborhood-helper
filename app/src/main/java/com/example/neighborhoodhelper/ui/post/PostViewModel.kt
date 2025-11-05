// PostViewModel.kt
// ViewModel that holds the create-post form state (text, optional image, urgent flag) and exposes submission method.

package com.example.neighborhoodhelper.ui.post

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostData(
    val authorId: String = "user_123",
    val authorName: String = "John Doe",
    val authorAvatarUrl: String? = null,
    val text: String = "",
    val imageBitmap: Bitmap? = null,
    val isUrgent: Boolean = false
)

class PostViewModel : ViewModel() {
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap: StateFlow<Bitmap?> = _imageBitmap.asStateFlow()

    private val _isUrgent = MutableStateFlow(false)
    val isUrgent: StateFlow<Boolean> = _isUrgent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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

    fun submitPost(onSubmit: (PostData) -> Unit) {
        val post = PostData(
            text = _text.value.trim(),
            imageBitmap = _imageBitmap.value,
            isUrgent = _isUrgent.value
        )
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: send to repository / backend
                onSubmit(post)
            } finally {
                _isLoading.value = false
            }
            // clear form after submission if desired
            clear()
        }
    }
}

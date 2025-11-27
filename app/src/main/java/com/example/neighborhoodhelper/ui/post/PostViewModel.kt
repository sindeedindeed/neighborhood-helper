// ViewModel that holds Create Post form state and submits PostRecord objects.
// This file intentionally uses a small in-memory PostRepository by default so the module
// can compile and run without adding Firestore dependencies. Replace the repository
// implementation with a Firestore-backed one when ready.
package com.example.neighborhoodhelper.ui.post

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// Data class representing a post stored in Firestore (or local repo)
data class PostRecord(
    val id: String = "",
    val text: String = "",
    val isUrgent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/** Simple repository contract to submit posts. */
interface PostRepository {
    fun submit(record: PostRecord, callback: (Result<PostRecord>) -> Unit)
}

/**
 * In-memory fake repository used for development and to keep the module runnable
 * without Firebase dependencies. It simulates a short network delay and succeeds.
 */
class FakePostRepository : PostRepository {
    private val handler = Handler(Looper.getMainLooper())

    override fun submit(record: PostRecord, callback: (Result<PostRecord>) -> Unit) {
        // simulate network delay
        handler.postDelayed({
            callback(Result.success(record))
        }, 500)
    }
}

class PostViewModel(application: Application, private val repo: PostRepository = FakePostRepository()) : AndroidViewModel(application) {
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

    /**
     * Submit post: delegates to PostRepository. The default repo is a fake implementation
     * that immediately returns success after a short delay. Replace repo with a Firestore
     * implementation when Firebase is added to the project.
     */
    fun submitPost(onResult: (Result<PostRecord>) -> Unit) {
        val id = UUID.randomUUID().toString()

        val record = PostRecord(
            id = id,
            text = _text.value.trim(),
            isUrgent = _isUrgent.value
        )

        _isLoading.value = true

        repo.submit(record) { result ->
            _isLoading.value = false
            if (result.isSuccess) {
                clear()
            }
            onResult(result)
        }
    }
}
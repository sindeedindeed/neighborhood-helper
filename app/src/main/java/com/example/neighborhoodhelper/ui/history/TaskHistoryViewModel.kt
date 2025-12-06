package com.example.neighborhoodhelper.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.TaskHistory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TaskHistoryViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _taskHistory = MutableStateFlow<List<TaskHistory>>(emptyList())
    val taskHistory: StateFlow<List<TaskHistory>> = _taskHistory

    init {
        loadTaskHistory()
    }

    private fun loadTaskHistory() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val result = repository.getTaskHistory(userId)
            result.onSuccess { history ->
                _taskHistory.value = history
            }.onFailure { error ->
                Log.e("TaskHistoryViewModel", "Error loading task history", error)
            }
        }
    }

    fun submitRating(task: TaskHistory, rating: Float, review: String, isRequester: Boolean) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val toUserId = if (isRequester) task.helperId else task.requesterId

            // Submit rating to userRatings collection
            repository.submitRating(
                postId = task.postId,
                toUserId = toUserId,
                rating = rating,
                review = review,
                category = task.category
            )

            // Update task history with rating
            repository.updateTaskHistoryRating(
                taskHistoryId = task.id,
                isRequester = isRequester,
                rating = rating,
                review = review
            )

            // Reload history
            loadTaskHistory()
        }
    }
}


package com.example.neighborhoodhelper.auth

import android.util.Log
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    private val repository = FirebaseRepository()

    suspend fun signInAnonymouslyIfNeeded(): Result<String> {
        return try {
            // Check if already signed in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                return Result.success(currentUser.uid)
            }

            // Sign in anonymously
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid ?: return Result.failure(Exception("Failed to get user ID"))

            // Create user profile with a default username
            val username = "User${userId.take(6)}"
            // CHANGED: Use the new function name
            repository.createOrUpdateUserProfile(
                username = username,
                email = "",
                phoneNumber = "",
                bio = ""
            )

            Log.d("AuthManager", "Signed in anonymously: $userId")
            Result.success(userId)
        } catch (e: Exception) {
            Log.e("AuthManager", "Error signing in", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isSignedIn(): Boolean = auth.currentUser != null
}
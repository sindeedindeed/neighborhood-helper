package com.example.neighborhoodhelper.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ImageUploadManager {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun uploadPostImage(imageUri: Uri, context: Context): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val fileName = "post_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child("posts")
                .child(userId)
                .child(fileName)

            // Upload the file
            storageRef.putFile(imageUri).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Log.d("ImageUpload", "Image uploaded successfully: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading image", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(imageUri: Uri, context: Context): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not authenticated"))
            val fileName = "profile_$userId.jpg"
            val storageRef = storage.reference
                .child("profiles")
                .child(fileName)

            // Upload the file
            storageRef.putFile(imageUri).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await()

            Log.d("ImageUpload", "Profile image uploaded: $downloadUrl")
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error uploading profile image", e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ImageUpload", "Error deleting image", e)
            Result.failure(e)
        }
    }
}

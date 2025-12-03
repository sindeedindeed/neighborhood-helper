package com.example.neighborhoodhelper.notifications

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationHelper {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val TAG = "NotificationHelper"

    private suspend fun getCurrentUsername(): String {
        return try {
            val userId = auth.currentUser?.uid ?: return "Someone"
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("username") ?: "Someone"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting username", e)
            "Someone"
        }
    }

    suspend fun sendCommentNotification(postId: String, postOwnerId: String, commentText: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val currentUserId = currentUser.uid

            if (currentUserId == postOwnerId) return

            val currentUsername = getCurrentUsername()
            val truncatedComment = if (commentText.length > 50)
                "${commentText.take(50)}..."
            else
                commentText

            val notification = hashMapOf(
                "userId" to postOwnerId,
                "fromUserId" to currentUserId,
                "fromUsername" to currentUsername,
                "type" to "COMMENT",
                "postId" to postId,
                "message" to "commented: \"$truncatedComment\"",
                "title" to currentUsername,
                "isRead" to false,
                "isViewed" to false,
                "requiresAction" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✅ Comment notification sent to $postOwnerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending comment notification", e)
        }
    }

    suspend fun sendWillingNotification(postId: String, postOwnerId: String, willingUserId: String) {
        try {
            val currentUser = auth.currentUser ?: return

            if (currentUser.uid == postOwnerId) return

            val currentUsername = getCurrentUsername()

            val notification = hashMapOf(
                "userId" to postOwnerId,
                "fromUserId" to willingUserId,
                "fromUsername" to currentUsername,
                "type" to "WILLING",
                "postId" to postId,
                "message" to "wants to help with your request",
                "title" to currentUsername,
                "isRead" to false,
                "isViewed" to false,
                "requiresAction" to true,
                "actionData" to mapOf(
                    "willingUserId" to willingUserId,
                    "postId" to postId
                ),
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✅ Willing notification sent to $postOwnerId with actions")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending willing notification", e)
        }
    }

    suspend fun sendRequestAcceptedNotification(postId: String, willingUserId: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val currentUsername = getCurrentUsername()

            val notification = hashMapOf(
                "userId" to willingUserId,
                "fromUserId" to currentUser.uid,
                "fromUsername" to currentUsername,
                "type" to "REQUEST_ACCEPTED",
                "postId" to postId,
                "message" to "accepted your offer to help!",
                "title" to currentUsername,
                "isRead" to false,
                "isViewed" to false,
                "requiresAction" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✅ Request accepted notification sent to $willingUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request accepted notification", e)
        }
    }

    suspend fun sendRequestRejectedNotification(postId: String, willingUserId: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val currentUsername = getCurrentUsername()

            val notification = hashMapOf(
                "userId" to willingUserId,
                "fromUserId" to currentUser.uid,
                "fromUsername" to currentUsername,
                "type" to "REQUEST_REJECTED",
                "postId" to postId,
                "message" to "declined your offer to help",
                "title" to currentUsername,
                "isRead" to false,
                "isViewed" to false,
                "requiresAction" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✅ Request rejected notification sent to $willingUserId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending request rejected notification", e)
        }
    }

    suspend fun sendLikeNotification(postId: String, postOwnerId: String) {
        try {
            val currentUser = auth.currentUser ?: return
            val currentUserId = currentUser.uid

            if (currentUserId == postOwnerId) return

            val currentUsername = getCurrentUsername()

            val notification = hashMapOf(
                "userId" to postOwnerId,
                "fromUserId" to currentUserId,
                "fromUsername" to currentUsername,
                "type" to "LIKE",
                "postId" to postId,
                "message" to "liked your post",
                "title" to currentUsername,
                "isRead" to false,
                "isViewed" to false,
                "requiresAction" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("notifications").add(notification).await()
            Log.d(TAG, "✅ Like notification sent to $postOwnerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending like notification", e)
        }
    }
}
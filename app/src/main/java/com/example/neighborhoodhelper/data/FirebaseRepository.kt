package com.example.neighborhoodhelper.data

import android.util.Log
import com.example.neighborhoodhelper.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val postsCollection = firestore.collection("posts")
    private val commentsCollection = firestore.collection("comments")
    private val notificationsCollection = firestore.collection("notifications")
    private val usersCollection = firestore.collection("users")

    // Get current user ID
    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Get current user
    suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting current user", e)
            null
        }
    }

    // ============ POST OPERATIONS ============

    // Listen to posts in real-time
    fun observePosts(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing posts", error)
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(
                        timestamp = formatTimestamp(doc.getTimestamp("createdAt"))
                    )
                } ?: emptyList()

                trySend(posts)
            }

        awaitClose { listener.remove() }
    }

    // Create a new post
    suspend fun createPost(
        content: String,
        imageUrl: String?,
        location: String?
    ): Result<String> {
        return try {
            val user = getCurrentUser() ?: return Result.failure(Exception("User not authenticated"))

            val post = hashMapOf(
                "userId" to user.id,
                "username" to user.username,
                "userAvatarUrl" to user.avatarUrl,
                "content" to content,
                "imageUrl" to imageUrl,
                "location" to location,
                "likes" to 0,
                "likedBy" to emptyList<String>(),
                "comments" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val docRef = postsCollection.add(post).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creating post", e)
            Result.failure(e)
        }
    }

    // Like/Unlike a post
    suspend fun toggleLike(postId: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            val user = getCurrentUser() ?: return Result.failure(Exception("User not found"))
            val postRef = postsCollection.document(postId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val likedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<String>()
                val postOwnerId = snapshot.getString("userId") ?: ""

                if (likedBy.contains(userId)) {
                    // Unlike
                    transaction.update(postRef, "likedBy", FieldValue.arrayRemove(userId))
                    transaction.update(postRef, "likes", FieldValue.increment(-1))
                } else {
                    // Like
                    transaction.update(postRef, "likedBy", FieldValue.arrayUnion(userId))
                    transaction.update(postRef, "likes", FieldValue.increment(1))

                    // Create notification if not liking own post
                    if (postOwnerId != userId) {
                        val notification = hashMapOf(
                            "userId" to postOwnerId,
                            "fromUserId" to userId,
                            "fromUsername" to user.username,
                            "type" to "LIKE",
                            "postId" to postId,
                            "message" to "${user.username} liked your post",
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                        notificationsCollection.add(notification)
                    }
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error toggling like", e)
            Result.failure(e)
        }
    }

    // ============ COMMENT OPERATIONS ============

    // Listen to comments for a specific post
    fun observeComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = commentsCollection
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing comments", error)
                    close(error)
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(
                        timestamp = formatTimestamp(doc.getTimestamp("createdAt"))
                    )
                } ?: emptyList()

                trySend(comments)
            }

        awaitClose { listener.remove() }
    }

    // Add a comment
    suspend fun addComment(postId: String, text: String): Result<String> {
        return try {
            val user = getCurrentUser() ?: return Result.failure(Exception("User not authenticated"))

            val comment = hashMapOf(
                "postId" to postId,
                "userId" to user.id,
                "author" to user.username,
                "authorAvatarUrl" to user.avatarUrl,
                "text" to text,
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Add comment
            val commentRef = commentsCollection.add(comment).await()

            // Increment comment count
            postsCollection.document(postId)
                .update("comments", FieldValue.increment(1))
                .await()

            // Get post owner to send notification
            val postDoc = postsCollection.document(postId).get().await()
            val postOwnerId = postDoc.getString("userId") ?: ""

            // Create notification if not commenting on own post
            if (postOwnerId != user.id) {
                val notification = hashMapOf(
                    "userId" to postOwnerId,
                    "fromUserId" to user.id,
                    "fromUsername" to user.username,
                    "type" to "COMMENT",
                    "postId" to postId,
                    "commentId" to commentRef.id,
                    "message" to "${user.username} commented on your post",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                notificationsCollection.add(notification)
            }

            Result.success(commentRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error adding comment", e)
            Result.failure(e)
        }
    }

    // ============ NOTIFICATION OPERATIONS ============

    // Listen to notifications for current user
    fun observeNotifications(): Flow<List<Notification>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = notificationsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing notifications", error)
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)
                } ?: emptyList()

                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }

    // Mark notification as read
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error marking notification as read", e)
            Result.failure(e)
        }
    }

    // ============ USER OPERATIONS ============

    // Create or update user profile
    // Update the existing createOrUpdateUser function
    suspend fun createOrUpdateUserProfile(
        username: String,
        email: String,
        phoneNumber: String = "",
        bio: String = "",
        avatarUrl: String = ""
    ): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            val user = hashMapOf(
                "username" to username,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "bio" to bio,
                "avatarUrl" to avatarUrl,
                "friends" to emptyList<String>(),
                "friendRequests" to emptyList<String>()
            )

            usersCollection.document(userId).set(user).await()
            Log.d("FirebaseRepo", "User profile created/updated: $username")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error creating/updating user", e)
            Result.failure(e)
        }
    }

    // Check if user has completed profile
    suspend fun hasCompletedProfile(): Boolean {
        return try {
            val user = getCurrentUser()
            user != null && user.username.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }

    // ============ HELPER FUNCTIONS ============

    private fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "Just now"

        val now = Date()
        val diff = now.time - timestamp.toDate().time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
        }
    }
}

package com.example.neighborhoodhelper.data

import android.util.Log
import com.example.neighborhoodhelper.model.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.neighborhoodhelper.data.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.get
import com.example.neighborhoodhelper.notifications.NotificationHelper
import kotlin.collections.remove
import kotlin.text.get

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
        location: String?,
        category: String = "OTHER"
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
                "category" to category,
                "status" to "active",
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

            // Send notification using NotificationHelper
            if (postOwnerId != user.id) {
                NotificationHelper.sendCommentNotification(postId, postOwnerId, text)
            }

            Result.success(commentRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error adding comment", e)
            Result.failure(e)
        }
    }


    // ============ NOTIFICATION OPERATIONS ============

    // Listen to notifications for current user
    fun observeNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@callbackFlow

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)  // Fixed: changed from 'timestamp' to 'createdAt'
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error observing notifications", error)
                    return@addSnapshotListener
                }

                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d("FirebaseRepository", "✅ Loaded ${notifications.size} notifications for user $userId")
                trySend(notifications)
            }

        awaitClose { listener.remove() }
    }


    // Mark notification as read
    suspend fun markNotificationAsRead(notificationId: String) {
        try {
            firestore.collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error marking notification as read", e)
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
    // ============ FRIEND OPERATIONS ============

    // Search users by username
    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .orderBy("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }.filter { it.id != getCurrentUserId() } // Exclude current user

            Result.success(users)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error searching users", e)
            Result.failure(e)
        }
    }

    // Send friend request
    suspend fun sendFriendRequest(toUserId: String): Result<String> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not authenticated"))
            val toUser = usersCollection.document(toUserId).get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("User not found"))

            val friendRequest = hashMapOf(
                "fromUserId" to currentUser.id,
                "fromUsername" to currentUser.username,
                "fromAvatarUrl" to currentUser.avatarUrl,
                "toUserId" to toUserId,
                "toUsername" to toUser.username,
                "status" to "PENDING",
                "createdAt" to FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection("friendRequests").add(friendRequest).await()

            // Update user's pendingRequests list
            usersCollection.document(currentUser.id)
                .update("pendingRequests", FieldValue.arrayUnion(docRef.id))
                .await()

            // Update recipient's friendRequests list
            usersCollection.document(toUserId)
                .update("friendRequests", FieldValue.arrayUnion(docRef.id))
                .await()

            // Create notification
            val notification = hashMapOf(
                "userId" to toUserId,
                "fromUserId" to currentUser.id,
                "fromUsername" to currentUser.username,
                "type" to "FRIEND_REQUEST",
                "message" to "${currentUser.username} sent you a friend request",
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )
            notificationsCollection.add(notification)

            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error sending friend request", e)
            Result.failure(e)
        }
    }
    suspend fun markAllNotificationsAsViewed() {
        try {
            val userId = auth.currentUser?.uid ?: return

            val notifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isViewed", false)
                .get()
                .await()

            notifications.documents.forEach { doc ->
                doc.reference.update("isViewed", true).await()
            }

            Log.d("FirebaseRepository", "✅ All notifications marked as viewed")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error marking all notifications as viewed", e)
        }
    }


    // Get friend requests for current user
    fun observeFriendRequests(): Flow<List<FriendRequest>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("friendRequests")
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing friend requests", error)
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FriendRequest::class.java)
                } ?: emptyList()

                trySend(requests)
            }

        awaitClose { listener.remove() }
    }

    // Accept friend request
    suspend fun acceptFriendRequest(requestId: String, fromUserId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("friendRequests").document(requestId)

                // Update request status
                transaction.update(requestRef, "status", "ACCEPTED")

                // Add each user to the other's friends list
                val currentUserRef = usersCollection.document(currentUserId)
                val friendUserRef = usersCollection.document(fromUserId)

                transaction.update(currentUserRef, "friends", FieldValue.arrayUnion(fromUserId))
                transaction.update(currentUserRef, "friendRequests", FieldValue.arrayRemove(requestId))

                transaction.update(friendUserRef, "friends", FieldValue.arrayUnion(currentUserId))
                transaction.update(friendUserRef, "pendingRequests", FieldValue.arrayRemove(requestId))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    // Reject friend request
    suspend fun rejectFriendRequest(requestId: String, fromUserId: String): Result<Unit> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            firestore.runTransaction { transaction ->
                val requestRef = firestore.collection("friendRequests").document(requestId)

                // Update request status
                transaction.update(requestRef, "status", "REJECTED")

                // Remove from lists
                val currentUserRef = usersCollection.document(currentUserId)
                val friendUserRef = usersCollection.document(fromUserId)

                transaction.update(currentUserRef, "friendRequests", FieldValue.arrayRemove(requestId))
                transaction.update(friendUserRef, "pendingRequests", FieldValue.arrayRemove(requestId))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error rejecting friend request", e)
            Result.failure(e)
        }
    }

    // Get user's friends
    suspend fun getFriends(): Result<List<User>> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not authenticated"))
            val friendIds = currentUser.friends

            if (friendIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val friends = friendIds.mapNotNull { friendId ->
                try {
                    usersCollection.document(friendId).get().await().toObject(User::class.java)
                } catch (e: Exception) {
                    Log.w("FirebaseRepo", "Error getting friend $friendId", e)
                    null
                }
            }

            Result.success(friends)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting friends", e)
            Result.failure(e)
        }
    }

    // Get posts from friends only
    fun observeFriendsPosts(): Flow<List<Post>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // First get current user's friends list
        val currentUser = getCurrentUser()
        val friendIds = currentUser?.friends ?: emptyList()

        if (friendIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = postsCollection
            .whereIn("userId", friendIds)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing friends posts", error)
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

    // ============ CHAT OPERATIONS ============

    // Get or create chat room between two users
    suspend fun getOrCreateChatRoom(otherUserId: String): Result<String> {
        return try {
            val currentUserId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not found"))
            val otherUser = usersCollection.document(otherUserId).get().await().toObject(User::class.java)
                ?: return Result.failure(Exception("Other user not found"))

            // Check if room already exists
            val existingRooms = firestore.collection("chatRooms")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()

            val existingRoom = existingRooms.documents.firstOrNull { doc ->
                val participants = doc.get("participants") as? List<*> ?: emptyList<String>()
                participants.contains(otherUserId)
            }

            if (existingRoom != null) {
                return Result.success(existingRoom.id)
            }

            // Create new room
            val chatRoom = hashMapOf(
                "participants" to listOf(currentUserId, otherUserId),
                "participantNames" to mapOf(
                    currentUserId to currentUser.username,
                    otherUserId to otherUser.username
                ),
                "participantAvatars" to mapOf(
                    currentUserId to currentUser.avatarUrl,
                    otherUserId to otherUser.avatarUrl
                ),
                "lastMessage" to "",
                "lastMessageSenderId" to "",
                "lastMessageTimestamp" to null,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val docRef = firestore.collection("chatRooms").add(chatRoom).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting/creating chat room", e)
            Result.failure(e)
        }
    }

    // Get user's chat rooms
    fun observeChatRooms(): Flow<List<ChatRoom>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chatRooms")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing chat rooms", error)
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)
                } ?: emptyList()

                trySend(rooms)
            }

        awaitClose { listener.remove() }
    }

    // Send message
    suspend fun sendMessage(roomId: String, text: String, imageUrl: String? = null): Result<String> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not authenticated"))

            val message = hashMapOf(
                "roomId" to roomId,
                "senderId" to currentUser.id,
                "senderName" to currentUser.username,
                "text" to text,
                "imageUrl" to imageUrl,
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )

            val messageRef = firestore.collection("messages").add(message).await()

            // Update chat room's last message
            firestore.collection("chatRooms").document(roomId).update(
                mapOf(
                    "lastMessage" to text,
                    "lastMessageSenderId" to currentUser.id,
                    "lastMessageTimestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(messageRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error sending message", e)
            Result.failure(e)
        }
    }

    // Observe messages in a room
    fun observeMessages(roomId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("messages")
            .whereEqualTo("roomId", roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing messages", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // ============ SETTINGS OPERATIONS ============

    // Get user settings
    suspend fun getUserSettings(): Result<UserSettings> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            val doc = firestore.collection("userSettings").document(userId).get().await()

            if (doc.exists()) {
                val settings = doc.toObject(UserSettings::class.java) ?: UserSettings(userId = userId)
                Result.success(settings)
            } else {
                // Create default settings
                val defaultSettings = UserSettings(userId = userId)
                firestore.collection("userSettings").document(userId).set(defaultSettings).await()
                Result.success(defaultSettings)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting user settings", e)
            Result.failure(e)
        }
    }

    suspend fun toggleWilling(postId: String): Result<Boolean> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val postRef = firestore.collection("posts").document(postId)  // Changed from db to firestore

            val postSnapshot = postRef.get().await()
            val willingUsers = postSnapshot.get("willingUsers") as? List<String> ?: emptyList()

            val isCurrentlyWilling = willingUsers.contains(currentUserId)

            if (isCurrentlyWilling) {
                // Remove user from willing list
                postRef.update(
                    mapOf(
                        "willingUsers" to FieldValue.arrayRemove(currentUserId),
                        "likes" to FieldValue.increment(-1)  // Fixed: Use FieldValue.increment
                    )
                ).await()
                Result.success(false)
            } else {
                // Add user to willing list
                postRef.update(
                    mapOf(
                        "willingUsers" to FieldValue.arrayUnion(currentUserId),
                        "likes" to FieldValue.increment(1)  // Fixed: Use FieldValue.increment
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Update user settings
    suspend fun updateUserSettings(settings: UserSettings): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            firestore.collection("userSettings").document(userId).set(settings).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating settings", e)
            Result.failure(e)
        }
    }


    // Update FCM token
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.failure(Exception("User not authenticated"))

            usersCollection.document(userId).update("fcmToken", token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    // Add willing user with location



    // Accept willing user
    suspend fun acceptWillingUser(
        postId: String,
        willingUserId: String
    ): Result<WillingUser> = withContext(Dispatchers.IO) {
        try {
            val postRef = firestore.collection("posts").document(postId)
            val postSnapshot = postRef.get().await()

            val willingUserDetails = postSnapshot.get("willingUserDetails") as? List<Map<String, Any>> ?: emptyList()
            val willingUser = willingUserDetails.find {
                it["userId"] == willingUserId
            }?.let { map ->
                WillingUser(
                    userId = map["userId"] as? String ?: "",
                    userName = map["userName"] as? String ?: "",
                    userProfileUrl = map["userProfileUrl"] as? String,
                    latitude = map["latitude"] as? Double ?: 0.0,
                    longitude = map["longitude"] as? Double ?: 0.0,
                    address = map["address"] as? String ?: "",
                    status = "accepted"
                )
            } ?: return@withContext Result.failure(Exception("User not found"))

            postRef.update(mapOf("status" to "matched")).await()
            // Send notification using NotificationHelper
            NotificationHelper.sendRequestAcceptedNotification(postId, willingUserId)


            Result.success(willingUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectWillingUser(
        postId: String,
        willingUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sendMatchRejectedNotification(willingUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Get willing user details
    suspend fun getWillingUserDetails(
        postId: String,
        userId: String
    ): Result<WillingUser> = withContext(Dispatchers.IO) {
        try {
            val postRef = firestore.collection("posts").document(postId)
            val postSnapshot = postRef.get().await()

            val willingUserDetails = postSnapshot.get("willingUserDetails") as? List<Map<String, Any>> ?: emptyList()
            val willingUser = willingUserDetails.find {
                it["userId"] == userId
            }?.let { map ->
                WillingUser(
                    userId = map["userId"] as? String ?: "",
                    userName = map["userName"] as? String ?: "",
                    userProfileUrl = map["userProfileUrl"] as? String,
                    latitude = map["latitude"] as? Double ?: 0.0,
                    longitude = map["longitude"] as? Double ?: 0.0,
                    address = map["address"] as? String ?: ""
                )
            } ?: return@withContext Result.failure(Exception("User not found"))

            Result.success(willingUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // Helper methods for notifications
    private suspend fun sendWillingNotification(
        ownerId: String,
        postId: String,
        willingUserName: String,
        postContent: String
    ) {
        val token = getUserFCMToken(ownerId) ?: return

        firestore.collection("notifications").add(
            mapOf(
                "to" to token,
                "type" to "WILLING",
                "postId" to postId,
                "willingUserId" to auth.currentUser?.uid,
                "willingUserName" to willingUserName,
                "postContent" to postContent
            )
        ).await()
    }

    private suspend fun sendMatchAcceptedNotification(willingUserId: String, postId: String) {
        val token = getUserFCMToken(willingUserId) ?: return
        val ownerName = auth.currentUser?.displayName ?: "The requester"

        firestore.collection("notifications").add(
            mapOf(
                "to" to token,
                "type" to "MATCH_ACCEPTED",
                "postId" to postId,
                "ownerName" to ownerName
            )
        ).await()
    }

    private suspend fun sendMatchRejectedNotification(willingUserId: String) {
        val token = getUserFCMToken(willingUserId) ?: return
        val ownerName = auth.currentUser?.displayName ?: "The requester"

        firestore.collection("notifications").add(
            mapOf(
                "to" to token,
                "type" to "MATCH_REJECTED",
                "ownerName" to ownerName
            )
        ).await()
    }
    suspend fun sendNotificationToUser(
        targetUserId: String,
        type: String,
        data: Map<String, String>
    ) {
        try {
            // Get the target user's FCM token
            val userDoc = firestore.collection("users")
                .document(targetUserId)
                .get()
                .await()

            val fcmToken = userDoc.getString("fcmToken") ?: return

            // In production, use Firebase Cloud Functions or your backend server
            // For now, we'll store in Firestore and use Cloud Functions trigger
            val notification = hashMapOf(
                "to" to fcmToken,
                "type" to type,
                "data" to data,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection("fcm_messages")
                .add(notification)
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error sending notification", e)
        }
    }

    // Add these methods to your FirebaseRepository class

    // Update the observeNotifications method

    // Get unread notification count
    fun observeUnreadNotificationCount(): Flow<Int> = callbackFlow {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error observing unread count", error)
                    trySend(0)
                    return@addSnapshotListener
                }

                val count = snapshot?.size() ?: 0
                trySend(count)
            }

        awaitClose { listener.remove() }
    }

    // Update addWillingUser to pass willingUserId
    suspend fun addWillingUser(
        postId: String,
        willingUser: WillingUser
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val postRef = firestore.collection("posts").document(postId)
            val postSnapshot = postRef.get().await()

            postRef.update(
                mapOf(
                    "willingUsers" to FieldValue.arrayUnion(currentUserId),
                    "willingUserDetails" to FieldValue.arrayUnion(
                        mapOf(
                            "userId" to willingUser.userId,
                            "userName" to willingUser.userName,
                            "userProfileUrl" to willingUser.userProfileUrl,
                            "latitude" to willingUser.latitude,
                            "longitude" to willingUser.longitude,
                            "address" to willingUser.address,
                            "status" to (willingUser.status ?: "pending")
                        )
                    )
                )
            ).await()

            val postOwnerId = postSnapshot.getString("userId")
                ?: return@withContext Result.failure(Exception("No owner"))

            // Send notification with willingUserId
            if (postOwnerId != currentUserId) {
                NotificationHelper.sendWillingNotification(postId, postOwnerId, currentUserId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error adding willing user", e)
            Result.failure(e)
        }
    }

    // Accept willing user request and create active match
    suspend fun acceptWillingUserRequest(
        notificationId: String,
        postId: String,
        willingUserId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseRepo", "🔵 Starting acceptWillingUserRequest - notifId: $notificationId, postId: $postId, willingUserId: $willingUserId")

            val currentUser = getCurrentUser()
            if (currentUser == null) {
                Log.e("FirebaseRepo", "❌ Not authenticated")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            Log.d("FirebaseRepo", "✅ Current user: ${currentUser.username}")

            // Mark notification as processed FIRST to ensure buttons disappear
            try {
                firestore.collection("notifications").document(notificationId)
                    .update(mapOf(
                        "isRead" to true,
                        "requiresAction" to false
                    ))
                    .await()
                Log.d("FirebaseRepo", "✅ Notification marked as processed")
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "⚠️ Failed to update notification, continuing anyway", e)
            }

            val postRef = firestore.collection("posts").document(postId)
            val postSnapshot = postRef.get().await()
            val post = postSnapshot.toObject(Post::class.java)
            if (post == null) {
                Log.e("FirebaseRepo", "❌ Post not found")
                return@withContext Result.failure(Exception("Post not found"))
            }
            Log.d("FirebaseRepo", "✅ Post found: ${post.content.take(30)}...")

            if (!post.activeMatchId.isNullOrEmpty()) {
                return@withContext Result.failure(Exception("This request already has an active helper."))
            }

            if (userHasOngoingMatch(post.userId)) {
                return@withContext Result.failure(Exception("Requester already has an active match."))
            }

            // Get helper user details directly from users collection
            val helperDoc = usersCollection.document(willingUserId).get().await()
            val helper = helperDoc.toObject(User::class.java)
            if (helper == null) {
                Log.e("FirebaseRepo", "❌ Helper user not found")
                return@withContext Result.failure(Exception("Helper not found"))
            }
            Log.d("FirebaseRepo", "✅ Helper found: ${helper.username}")

            if (userHasOngoingMatch(helper.id)) {
                return@withContext Result.failure(Exception("Helper is already working on another task."))
            }

            val helperLocation = (postSnapshot.get("willingUserDetails") as? List<Map<String, Any?>>)
                ?.firstOrNull { it["userId"] == willingUserId }
            val helperLat = (helperLocation?.get("latitude") as? Number)?.toDouble()
                ?: helper.lastKnownLatitude
                ?: helper.latitude
                ?: 0.0
            val helperLon = (helperLocation?.get("longitude") as? Number)?.toDouble()
                ?: helper.lastKnownLongitude
                ?: helper.longitude
                ?: 0.0

            // Update post status to matched
            try {
                postRef.update(mapOf(
                    "status" to "matched",
                    "matchedHelperId" to willingUserId,
                    "matchedHelperName" to helper.username
                )).await()
                Log.d("FirebaseRepo", "✅ Post updated to matched")
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "❌ Failed to update post status", e)
                return@withContext Result.failure(Exception("Permission denied: Cannot update post. ${e.message}"))
            }

            // Create ActiveMatch
            val activeMatch = hashMapOf(
                "postId" to postId,
                "requesterId" to post.userId,
                "requesterName" to post.username,
                "requesterPhone" to currentUser.phoneNumber,
                "requesterLat" to (post.latitude ?: 0.0),
                "requesterLon" to (post.longitude ?: 0.0),
                "helperId" to helper.id,
                "helperName" to helper.username,
                "helperPhone" to helper.phoneNumber,
                "helperLat" to helperLat,
                "helperLon" to helperLon,
                "helperRating" to helper.averageRating,
                "helperRatingCount" to helper.totalRatings,
                "requesterRating" to currentUser.averageRating,
                "requesterRatingCount" to currentUser.totalRatings,
                "status" to "active",
                "distance" to 0f,
                "isProximityReached" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastUpdated" to FieldValue.serverTimestamp()
            )

            val matchRef = try {
                firestore.collection("activeMatches").add(activeMatch).await()
            } catch (e: Exception) {
                Log.e("FirebaseRepo", "❌ Failed to create active match", e)
                return@withContext Result.failure(Exception("Permission denied: Cannot create match. ${e.message}"))
            }
            val matchId = matchRef.id
            Log.d("FirebaseRepo", "✅ ActiveMatch created: $matchId")

            // Update post with activeMatchId
            try {
                postRef.update("activeMatchId", matchId).await()
                Log.d("FirebaseRepo", "✅ Post updated with matchId")
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "⚠️ Failed to update post with matchId", e)
            }

            // Send acceptance notification
            try {
                NotificationHelper.sendRequestAcceptedNotification(postId, willingUserId)
                Log.d("FirebaseRepo", "✅ Acceptance notification sent")
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "⚠️ Failed to send notification", e)
            }

            Log.d("FirebaseRepo", "🎉 Accept willing completed successfully!")
            Result.success(matchId)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "❌ Error accepting willing user", e)
            Result.failure(e)
        }
    }

    // Reject willing user request
    suspend fun rejectWillingUserRequest(
        notificationId: String,
        postId: String,
        willingUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("FirebaseRepo", "Starting rejectWillingUserRequest - notifId: $notificationId")

            // Mark notification as read, remove action requirement, and mark as rejected
            firestore.collection("notifications").document(notificationId)
                .update(mapOf(
                    "isRead" to true,
                    "requiresAction" to false,
                    "wasRejected" to true
                ))
                .await()
            Log.d("FirebaseRepo", "Notification marked as rejected")

            // Send rejection notification
            try {
                NotificationHelper.sendRequestRejectedNotification(postId, willingUserId)
                Log.d("FirebaseRepo", "Rejection notification sent")
            } catch (e: Exception) {
                Log.w("FirebaseRepo", "Failed to send rejection notification", e)
            }

            Log.d("FirebaseRepo", "Reject willing completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error rejecting willing user", e)
            Result.failure(e)
        }
    }


    private suspend fun getUserFCMToken(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            userDoc.getString("fcmToken")
        } catch (e: Exception) {
            null
        }
    }

    // ============ RATING OPERATIONS ============

    suspend fun submitRating(
        postId: String,
        toUserId: String,
        rating: Float,
        review: String,
        category: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUser = getCurrentUser() ?: return@withContext Result.failure(Exception("Not authenticated"))

            val userRating = hashMapOf(
                "postId" to postId,
                "fromUserId" to currentUser.id,
                "fromUsername" to currentUser.username,
                "toUserId" to toUserId,
                "rating" to rating,
                "review" to review,
                "category" to category,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("userRatings").add(userRating).await()
            updateUserRating(toUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error submitting rating", e)
            Result.failure(e)
        }
    }

    private suspend fun updateUserRating(userId: String) {
        try {
            val ratingsSnapshot = firestore.collection("userRatings")
                .whereEqualTo("toUserId", userId)
                .get()
                .await()

            val ratings = ratingsSnapshot.documents.mapNotNull {
                it.getDouble("rating")?.toFloat()
            }

            if (ratings.isNotEmpty()) {
                val avgRating = ratings.average().toFloat()
                val totalRatings = ratings.size

                usersCollection.document(userId).update(
                    mapOf(
                        "averageRating" to avgRating,
                        "totalRatings" to totalRatings
                    )
                ).await()
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating user rating", e)
        }
    }

    // ============ TASK HISTORY OPERATIONS ============

    suspend fun getTaskHistory(userId: String): Result<List<TaskHistory>> = withContext(Dispatchers.IO) {
        try {
            val asRequester = firestore.collection("taskHistory")
                .whereEqualTo("requesterId", userId)
                .get()
                .await()

            val asHelper = firestore.collection("taskHistory")
                .whereEqualTo("helperId", userId)
                .get()
                .await()

            val history = (asRequester.documents + asHelper.documents)
                .mapNotNull { it.toObject(TaskHistory::class.java) }
                .sortedByDescending { it.completedAt }

            Result.success(history)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error getting task history", e)
            Result.failure(e)
        }
    }

    suspend fun updateTaskHistoryRating(
        taskHistoryId: String,
        isRequester: Boolean,
        rating: Float,
        review: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = if (isRequester) {
                mapOf("requesterRating" to rating, "requesterReview" to review)
            } else {
                mapOf("helperRating" to rating, "helperReview" to review)
            }

            firestore.collection("taskHistory").document(taskHistoryId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating task history rating", e)
            Result.failure(e)
        }
    }

    // ============ ACTIVE MATCH OPERATIONS ============

    private suspend fun userHasOngoingMatch(userId: String): Boolean {
        return try {
            val activeStatuses = setOf("active", "arrived")

            val helperSnapshot = firestore.collection("activeMatches")
                .whereEqualTo("helperId", userId)
                .get()
                .await()
            if (helperSnapshot.documents.any { (it.getString("status") ?: "active") in activeStatuses }) {
                return true
            }

            val requesterSnapshot = firestore.collection("activeMatches")
                .whereEqualTo("requesterId", userId)
                .get()
                .await()
            requesterSnapshot.documents.any { (it.getString("status") ?: "active") in activeStatuses }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error checking active matches for $userId", e)
            true
        }
    }

    fun observeActiveMatch(matchId: String): Flow<ActiveMatch?> = callbackFlow {
        val listener = firestore.collection("activeMatches").document(matchId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepo", "Error observing active match", error)
                    close(error)
                    return@addSnapshotListener
                }

                val match = snapshot?.toObject(ActiveMatch::class.java)
                trySend(match)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateMatchLocation(
        matchId: String,
        isHelper: Boolean,
        latitude: Double,
        longitude: Double,
        distance: Float
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = if (isHelper) {
                mapOf(
                    "helperLat" to latitude,
                    "helperLon" to longitude,
                    "distance" to distance,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "requesterLat" to latitude,
                    "requesterLon" to longitude,
                    "distance" to distance,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
            }

            firestore.collection("activeMatches").document(matchId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating match location", e)
            Result.failure(e)
        }
    }

    suspend fun updateMatchProximityReached(matchId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("activeMatches").document(matchId)
                .update(
                    mapOf(
                        "isProximityReached" to true,
                        "status" to "arrived"
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error updating proximity status", e)
            Result.failure(e)
        }
    }

    suspend fun completeActiveMatch(matchId: String, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firestore.collection("activeMatches").document(matchId)
                .update("status", "completed")
                .await()

            postsCollection.document(postId)
                .update("status", "completed")
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error completing match", e)
            Result.failure(e)
        }
    }

    // Observe all active matches for current user (as requester or helper)
    fun observeActiveMatchesForUser(userId: String): Flow<List<ActiveMatch>> = callbackFlow {
        val listener = firestore.collection("activeMatches")
            .whereEqualTo("requesterId", userId)
            .addSnapshotListener { requesterSnapshot, requesterError ->
                if (requesterError != null) {
                    Log.e("FirebaseRepo", "Error observing requester matches", requesterError)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                firestore.collection("activeMatches")
                    .whereEqualTo("helperId", userId)
                    .addSnapshotListener { helperSnapshot, helperError ->
                        if (helperError != null) {
                            Log.e("FirebaseRepo", "Error observing helper matches", helperError)
                            trySend(requesterSnapshot?.documents?.mapNotNull { it.toObject(ActiveMatch::class.java) } ?: emptyList())
                            return@addSnapshotListener
                        }

                        val matches = mutableListOf<ActiveMatch>()
                        requesterSnapshot?.documents?.mapNotNullTo(matches) { it.toObject(ActiveMatch::class.java) }
                        helperSnapshot?.documents?.mapNotNullTo(matches) { it.toObject(ActiveMatch::class.java) }

                        Log.d("FirebaseRepo", "Loaded ${matches.size} active matches for user $userId")
                        trySend(matches)
                    }
            }

        awaitClose { listener.remove() }
    }
}

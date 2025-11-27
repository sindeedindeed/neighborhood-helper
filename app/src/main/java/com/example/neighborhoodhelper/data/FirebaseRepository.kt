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
}

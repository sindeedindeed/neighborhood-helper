package com.example.neighborhoodhelper.fcm

import android.app.NotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.neighborhoodhelper.MainActivity
import com.example.neighborhoodhelper.R
import com.example.neighborhoodhelper.receivers.NotificationActionReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.data.let { data ->
            val type = data["type"] ?: return

            when (type) {
                "WILLING" -> handleWillingNotification(data)
                "MATCH_ACCEPTED" -> handleMatchAccepted(data)
                "MATCH_REJECTED" -> handleMatchRejected(data)
            }
        }
    }

    private fun handleWillingNotification(data: Map<String, String>) {
        val postId = data["postId"] ?: return
        val willingUserId = data["willingUserId"] ?: return
        val willingUserName = data["willingUserName"] ?: "Someone"
        val postContent = data["postContent"] ?: "your post"

        createNotificationWithActions(
            postId = postId,
            willingUserId = willingUserId,
            willingUserName = willingUserName,
            title = "Someone wants to help!",
            body = "$willingUserName is willing to help with: $postContent"
        )
    }

    private fun handleMatchAccepted(data: Map<String, String>) {
        val postId = data["postId"] ?: return
        val ownerName = data["ownerName"] ?: "The requester"

        showSimpleNotification(
            title = "Match Accepted!",
            body = "$ownerName accepted your help offer. Check the app for details.",
            postId = postId
        )
    }

    private fun handleMatchRejected(data: Map<String, String>) {
        val ownerName = data["ownerName"] ?: "The requester"

        showSimpleNotification(
            title = "Request Declined",
            body = "$ownerName declined your help offer.",
            postId = null
        )
    }

    private fun createNotificationWithActions(
        postId: String,
        willingUserId: String,
        willingUserName: String,
        title: String,
        body: String
    ) {
        val channelId = "willing_notifications"
        createNotificationChannel(channelId, "Willing to Help")

        // Accept action
        val acceptIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_ACCEPT"
            putExtra("postId", postId)
            putExtra("willingUserId", willingUserId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this, 0, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reject action
        val rejectIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "ACTION_REJECT"
            putExtra("postId", postId)
            putExtra("willingUserId", willingUserId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this, 1, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Tap notification intent
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("postId", postId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this, 2, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .addAction(android.R.drawable.ic_menu_add, "Accept", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Reject", rejectPendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showSimpleNotification(title: String, body: String, postId: String?) {
        val channelId = "match_notifications"
        createNotificationChannel(channelId, "Match Updates")

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            postId?.let { putExtra("postId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for $channelName"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .await()
                Log.d("FCM", "Token updated successfully")
            } catch (e: Exception) {
                Log.e("FCM", "Error saving token", e)
            }
        }
    }


}

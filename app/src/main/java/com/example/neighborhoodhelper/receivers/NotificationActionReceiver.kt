package com.example.neighborhoodhelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.neighborhoodhelper.MainActivity
import com.example.neighborhoodhelper.data.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActionReceiver : BroadcastReceiver() {

    private val repository = FirebaseRepository()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        when (intent.action) {
            "ACTION_ACCEPT" -> {
                val notificationId = intent.getStringExtra("notificationId") ?: ""
                val postId = intent.getStringExtra("postId") ?: ""
                val willingUserId = intent.getStringExtra("willingUserId") ?: ""

                if (postId.isEmpty() || willingUserId.isEmpty()) {
                    Log.e("NotificationAction", "Missing required data: postId=$postId, willingUserId=$willingUserId")
                    pendingResult.finish()
                    return
                }

                Log.d("NotificationAction", "Accept clicked - notificationId=$notificationId, postId=$postId, willingUserId=$willingUserId")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // If no notificationId, try to find it
                        val finalNotificationId = if (notificationId.isEmpty()) {
                            repository.findWillingNotificationId(postId, willingUserId) ?: ""
                        } else {
                            notificationId
                        }

                        val result = repository.acceptWillingUserRequest(
                            finalNotificationId,
                            postId,
                            willingUserId
                        )

                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "✅ Match accepted!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                launchSuccessScreen(context, postId, willingUserId)
                            } else {
                                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                Toast.makeText(
                                    context,
                                    "❌ $errorMessage",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.e("NotificationAction", "Failed to accept: $errorMessage")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationAction", "Error accepting match", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "❌ Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            "ACTION_REJECT" -> {
                val notificationId = intent.getStringExtra("notificationId") ?: ""
                val postId = intent.getStringExtra("postId") ?: ""
                val willingUserId = intent.getStringExtra("willingUserId") ?: ""

                if (postId.isEmpty() || willingUserId.isEmpty()) {
                    Log.e("NotificationAction", "Missing required data for reject")
                    pendingResult.finish()
                    return
                }

                Log.d("NotificationAction", "Reject clicked - notificationId=$notificationId, postId=$postId, willingUserId=$willingUserId")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // If no notificationId, try to find it
                        val finalNotificationId = if (notificationId.isEmpty()) {
                            repository.findWillingNotificationId(postId, willingUserId) ?: ""
                        } else {
                            notificationId
                        }

                        val result = repository.rejectWillingUserRequest(
                            finalNotificationId,
                            postId,
                            willingUserId
                        )

                        withContext(Dispatchers.Main) {
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Offer declined",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                Toast.makeText(
                                    context,
                                    "❌ $errorMessage",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationAction", "Error rejecting match", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "❌ Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun launchSuccessScreen(context: Context, postId: String, willingUserId: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "success")
            putExtra("acceptedPostId", postId)
            putExtra("willingUserId", willingUserId)
        }
        context.startActivity(launchIntent)
    }
}
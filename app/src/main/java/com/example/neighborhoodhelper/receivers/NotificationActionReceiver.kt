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

                if (notificationId.isEmpty() || postId.isEmpty() || willingUserId.isEmpty()) {
                    Log.e("NotificationAction", "Missing required data")
                    pendingResult.finish()
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = repository.acceptWillingUserRequest(
                            notificationId,
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
                                Toast.makeText(
                                    context,
                                    "❌ Failed to accept match",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationAction", "Error accepting match", e)
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

            "ACTION_REJECT" -> {
                val notificationId = intent.getStringExtra("notificationId") ?: ""
                val postId = intent.getStringExtra("postId") ?: ""
                val willingUserId = intent.getStringExtra("willingUserId") ?: ""

                if (notificationId.isEmpty() || postId.isEmpty() || willingUserId.isEmpty()) {
                    Log.e("NotificationAction", "Missing required data")
                    pendingResult.finish()
                    return
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val result = repository.rejectWillingUserRequest(
                            notificationId,
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
                                Toast.makeText(
                                    context,
                                    "❌ Failed to reject match",
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
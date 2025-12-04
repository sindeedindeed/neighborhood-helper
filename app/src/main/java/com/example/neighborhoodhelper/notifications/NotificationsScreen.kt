package com.example.neighborhoodhelper.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import com.example.neighborhoodhelper.data.AppNotification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: Flow<List<AppNotification>>,
    onNotificationClick: (String) -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsViewed: () -> Unit,
    onAcceptWilling: (String, String, String) -> Unit,
    onRejectWilling: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val notificationsList by notifications.collectAsState(initial = emptyList())

    // Mark all as viewed when screen opens
    LaunchedEffect(Unit) {
        onMarkAllAsViewed()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        val unreadCount = notificationsList.count { !it.isRead }
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (notificationsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No notifications yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(notificationsList) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            notification.postId?.let { postId ->
                                onNotificationClick(postId)
                                onMarkAsRead(notification.id)
                            }
                        },
                        onAccept = {
                            val actionData = notification.actionData
                            if (actionData != null) {
                                val postId = actionData["postId"] ?: ""
                                val willingUserId = actionData["willingUserId"] ?: ""
                                onAcceptWilling(notification.id, postId, willingUserId)
                            }
                        },
                        onReject = {
                            val actionData = notification.actionData
                            if (actionData != null) {
                                val postId = actionData["postId"] ?: ""
                                val willingUserId = actionData["willingUserId"] ?: ""
                                onRejectWilling(notification.id, postId, willingUserId)
                            }
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: AppNotification,
    onClick: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        "COMMENT" -> Icons.Default.Comment
                        "WILLING" -> Icons.Default.VolunteerActivism  // Changed from Favorite (heart)
                        "REQUEST_ACCEPTED" -> Icons.Default.CheckCircle
                        "REQUEST_REJECTED" -> Icons.Default.Cancel
                        "LIKE" -> Icons.Default.ThumbUp
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        "COMMENT" -> MaterialTheme.colorScheme.primary
                        "WILLING" -> Color(0xFF4CAF50)  // Green for willing
                        "REQUEST_ACCEPTED" -> Color(0xFF51CF66)
                        "REQUEST_REJECTED" -> Color(0xFFFF6B6B)
                        "LIKE" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = notification.title.ifEmpty { "Notification" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF1A1A1A)
                        )
                        // Show REJECTED badge if notification was rejected
                        if (notification.wasRejected && notification.type == "WILLING") {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFFFF6B6B)
                            ) {
                                Text(
                                    text = "REJECTED",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = notification.getFormattedTime(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                }

                if (!notification.isRead) {
                    Box(
                        modifier = Modifier.size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Spacer(Modifier.size(12.dp))
                        }
                    }
                }
            }

            // Show Accept/Reject buttons for WILLING notifications
            if (notification.requiresAction && notification.type == "WILLING") {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept")
                    }
                }
            }
        }
    }
}
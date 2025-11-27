package com.example.neighborhoodhelper.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.neighborhoodhelper.model.ChatRoom
import com.example.neighborhoodhelper.ui.feed.PrimaryPurple
import com.example.neighborhoodhelper.ui.feed.MediumGray
import com.example.neighborhoodhelper.ui.feed.DarkGray
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onChatRoomClick: (String) -> Unit
) {
    val viewModel: ChatViewModel = viewModel()
    val chatRooms by viewModel.chatRooms.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple
                )
            )
        }
    ) { padding ->
        if (chatRooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "No chats",
                        tint = MediumGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No messages yet",
                        color = MediumGray,
                        fontSize = 16.sp
                    )
                    Text(
                        "Start a conversation with your friends",
                        color = MediumGray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatRooms, key = { it.id }) { room ->
                    ChatRoomItem(
                        room = room,
                        currentUserId = currentUserId ?: "",
                        onClick = { onChatRoomClick(room.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRoomItem(
    room: ChatRoom,
    currentUserId: String,
    onClick: () -> Unit
) {
    val otherUserId = room.participants.firstOrNull { it != currentUserId } ?: ""
    val otherUserName = room.participantNames[otherUserId] ?: "Unknown"
    val otherUserAvatar = room.participantAvatars[otherUserId] ?: ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (otherUserAvatar.isNotBlank()) {
                    AsyncImage(
                        model = otherUserAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = otherUserName.firstOrNull()?.toString()?.uppercase() ?: "U",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediumGray
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherUserName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGray
                )
                Text(
                    text = room.lastMessage.ifBlank { "Start a conversation" },
                    fontSize = 14.sp,
                    color = MediumGray,
                    maxLines = 1
                )
            }
        }
    }
}
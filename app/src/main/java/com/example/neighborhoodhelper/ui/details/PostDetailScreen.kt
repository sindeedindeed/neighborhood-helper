@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.neighborhoodhelper.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.neighborhoodhelper.ui.components.CommentItem
import com.example.neighborhoodhelper.ui.feed.FeedViewModel

val PrimaryPurple = Color(0xFF6B3FA0)
val LightBackground = Color(0xFFF5F5F5)
val DarkGray = Color(0xFF424242)
val MediumGray = Color(0xFF757575)

@Composable
fun PostDetailScreen(navController: NavController, postId: String) {
    val viewModel: FeedViewModel = viewModel()
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val post = postsState.value.firstOrNull { it.id == postId }
    val commentsState = viewModel.comments.collectAsStateWithLifecycle()
    val comments = commentsState.value.filter { it.postId == postId }

    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "Post Details",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp)
            )

            if (post == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Post not found", color = DarkGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Post Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // User Info
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0E0E0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = post.username.firstOrNull()?.toString() ?: "U",
                                            fontWeight = FontWeight.Bold,
                                            color = MediumGray,
                                            fontSize = 18.sp
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = post.username,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = post.timestamp,
                                            color = MediumGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                // Post Content
                                Text(
                                    text = post.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DarkGray
                                )

                                // Post Image
                                if (!post.imageUrl.isNullOrBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    val imageShape = RoundedCornerShape(12.dp)
                                    AsyncImage(
                                        model = post.imageUrl,
                                        contentDescription = "Post image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(250.dp)
                                            .clip(imageShape)
                                            .background(Color(0xFFE0E0E0))
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                // Location
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = "Location",
                                        tint = PrimaryPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        post.location ?: "Kolabagan, Dhaka",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MediumGray
                                    )
                                }
                            }
                        }
                    }

                    // Comments Section Header
                    item {
                        Text(
                            text = "Comments (${comments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Show all comments
                    if (comments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No comments yet. Be the first to comment!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MediumGray
                                    )
                                }
                            }
                        }
                    } else {
                        items(comments) { comment ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                CommentItem(comment = comment)
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Willing action */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryPurple
                        )
                    ) {
                        Text("Willing")
                    }

                    Button(
                        onClick = { showCommentInput = !showCommentInput },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Comment")
                    }
                }

                AnimatedVisibility(visible = showCommentInput) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Write a comment...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple
                            )
                        )

                        IconButton(onClick = {
                            val toSend = commentText.trim()
                            if (toSend.isNotBlank()) {
                                viewModel.addComment(postId = postId, author = "You", text = toSend)
                                commentText = ""
                                showCommentInput = false
                            }
                        }) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = PrimaryPurple
                            )
                        }
                    }
                }
            }
        }
    }
}
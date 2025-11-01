@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.neighborhoodhelper.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.navigation.NavController
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import com.example.neighborhoodhelper.ui.feed.FeedViewModel
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import com.example.neighborhoodhelper.ui.components.CommentItem
import coil.compose.AsyncImage

@Composable
fun PostDetailScreen(navController: NavController, postId: String) {
    val viewModel: FeedViewModel = viewModel()
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val post = postsState.value.firstOrNull { it.id == postId }
    val commentsState = viewModel.comments.collectAsStateWithLifecycle()
    val comments = commentsState.value.filter { it.postId == postId }

    // control comment input visibility & text
    var showCommentInput by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        containerColor = Color(0xFFF8FAFC), // light off-white background
        topBar = {
            TopAppBar(
                title = { Text("Post Details", color = Color.White, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D47A1), titleContentColor = Color.White),
                modifier = Modifier.shadow(4.dp)
            )
        },
        bottomBar = {
            // bottom action bar always visible — use a Box with background to avoid Surface issues
            Box(modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { /* no-op Willing */ },
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D47A1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D47A1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Willing")
                    }

                    Button(
                        onClick = { showCommentInput = true },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726), contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
                        Spacer(Modifier.width(8.dp))
                        Text("Comment")
                    }
                }
            }
        }
    ) { paddingValues ->
        // Use a Box so we can anchor the expanding comment input to the bottom using .align
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            if (post == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Post not found", color = MaterialTheme.colorScheme.onBackground)
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                ) {
                    // Image
                    val imageShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    if (!post.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.imageUrl,
                            contentDescription = "post image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(imageShape)
                                .shadow(6.dp, shape = imageShape)
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(imageShape)
                                .shadow(6.dp, shape = imageShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No image available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // User info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val initial = post.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            Text(initial, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = post.username,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            )
                            Text(
                                text = post.timestamp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color(0xFF263238))
                        Spacer(Modifier.width(8.dp))
                        Text("Kolabagan, Dhaka", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF546E7A))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full content
                    Text(text = post.content, style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comments section title
                    Text(text = "Comments", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Comments list
                    if (comments.isEmpty()) {
                        Text("No comments yet. Be the first to comment!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxHeight(), contentPadding = PaddingValues(bottom = 60.dp)) {
                            items(comments) { comment ->
                                CommentItem(comment = comment)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }

            // Expanding comment input anchored above bottom bar
            AnimatedVisibility(visible = showCommentInput) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .background(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
                    .padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text("Write a comment...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = false,
                            maxLines = 4
                        )
                        IconButton(onClick = {
                            val toSend = commentText.trim()
                            if (toSend.isNotBlank()) {
                                viewModel.addComment(postId = postId, author = "You", text = toSend)
                                commentText = ""
                                showCommentInput = false
                                focusManager.clearFocus()
                                keyboard?.hide()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

// Keep PostDetailContent preview for quick previewing
@Preview(showBackground = true)
@Composable
private fun PostDetailPreview() {
    val sample = Post(
        id = "sample",
        username = "Maishan Nadis",
        userAvatarUrl = "",
        timestamp = "5m",
        content = "Maishan lost his cat near Kolabagan. Please check your balconies and local shops. He is small, gray, and answers to \"Mishi\".",
        imageUrl = null,
        likes = 10,
        comments = 7
    )

    val sampleComments = listOf(
        Comment(id = "c1", postId = "sample", author = "Ayesha", text = "I can help look for the cat near the market.", timestamp = "5m"),
        Comment(id = "c2", postId = "sample", author = "Rafi", text = "I saw a similar cat yesterday.", timestamp = "10m")
    )

    NeighborhoodHelperTheme(dynamicColor = false) {
        // Show the content only (PostDetailContent previously) — use the same UI composition inline for preview
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC)).padding(16.dp)) {
            // Small preview of top part
            Text(sample.username, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(sample.content, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Text("Comments", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            sampleComments.forEach { CommentItem(username = it.author, text = it.text, time = it.timestamp) }
        }
    }
}

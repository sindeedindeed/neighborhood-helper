@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.neighborhoodhelper.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment
import com.example.neighborhoodhelper.ui.components.CommentItem
import com.example.neighborhoodhelper.ui.components.Avatar
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import androidx.compose.material3.HorizontalDivider

@Composable
fun FeedScreen(navController: NavController) {
    val viewModel: FeedViewModel = viewModel()
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value
    val commentsState = viewModel.comments.collectAsStateWithLifecycle()
    val comments = commentsState.value

    FeedContent(
        posts = posts,
        comments = comments,
        onWilling = { viewModel.accept(it) },
        onAddComment = { postId, author, text -> viewModel.addComment(postId, author, text) },
        onPostClick = { id -> navController.navigate("postDetail/$id") }
    )
}

@Composable
fun FeedContent(
    posts: List<Post>,
    comments: List<Comment>,
    onWilling: (postId: String) -> Unit,
    onAddComment: (postId: String, author: String, text: String) -> Unit,
    onPostClick: (postId: String) -> Unit
) {
    // track which post's comment box is open
    val openMap = remember { mutableStateMapOf<String, Boolean>() }
    // track input text per post
    val inputMap = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(4.dp),
                title = {
                    Text(
                        "Neighborhood Feed",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        // subtle background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(posts, key = { _, item -> item.id }) { index, post ->
                    // Animated appearance per item
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(durationMillis = 350, delayMillis = index * 50)) +
                                slideInVertically(animationSpec = tween(durationMillis = 350, delayMillis = index * 50))
                    ) {
                        Column {
                            PostCard(
                                post = post,
                                onWilling = { onWilling(post.id) },
                                onCommentClick = {
                                    // toggle input
                                    openMap[post.id] = !(openMap[post.id] ?: false)
                                },
                                modifier = Modifier
                                    .clickable { onPostClick(post.id) }
                            )

                            // comments list
                            val postComments = comments.filter { it.postId == post.id }
                            if (postComments.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(start = 56.dp, top = 8.dp)) {
                                    postComments.forEach { comment ->
                                        CommentItem(comment = comment)
                                    }
                                }
                            }

                            // inline comment box
                            val isOpen = openMap[post.id] ?: false
                            AnimatedVisibility(visible = isOpen) {
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val text = inputMap[post.id] ?: ""
                                    OutlinedTextField(
                                        value = text,
                                        onValueChange = { inputMap[post.id] = it },
                                        placeholder = { Text("Write a comment...") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = false,
                                        maxLines = 4
                                    )
                                    IconButton(onClick = {
                                        val toSend = inputMap[post.id]?.trim() ?: ""
                                        if (toSend.isNotBlank()) {
                                            onAddComment(post.id, "You", toSend)
                                            inputMap[post.id] = ""
                                            openMap[post.id] = false
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                                    }
                                }
                            }
                        }
                    }

                    if (index < posts.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onWilling: () -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Avatar + Username + Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(url = post.userAvatarUrl)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = post.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content summary (2-3 lines)
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3
                )
            }

            // Post image (if present) or nice placeholder
            val imageShape = RoundedCornerShape(12.dp)
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .clip(imageShape)
                        .background(MaterialTheme.colorScheme.surface)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Actions row: Willing (left) + Comments (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onWilling,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Willing")
                }

                Button(
                    onClick = onCommentClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments")
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Comments (${post.comments})")
                }
            }
        }
    }
}

// Previews updated to pass comments
@Preview(showBackground = true, name = "FeedScreenPreview")
@Composable
fun FeedScreenPreview() {
    // Provide sample posts for preview so we don't need a ViewModel in the preview.
    val samplePosts = listOf(
        Post(
            id = "1",
            username = "Maishan Nadis",
            userAvatarUrl = "",
            timestamp = "2m",
            content = "Lost cat near Kalabagan Please keep an eye out!",
            imageUrl = null,
            likes = 4,
            comments = 2
        ),
        Post(
            id = "2",
            username = "Faiza Tashmeah",
            userAvatarUrl = "",
            timestamp = "15m",
            content = "Anyone has a charger-fan I can borrow this afternoon?",
            imageUrl = null,
            likes = 1,
            comments = 5
        ),
        Post(
            id = "3",
            username = "Safwat Bushra",
            userAvatarUrl = "",
            timestamp = "1h",
            content = "Need a Math tutor for my cousin. Any recommendations?",
            imageUrl = null,
            likes = 12,
            comments = 3
        )
    )

    val sampleComments = listOf(
        Comment(id = "c1", postId = "1", author = "Ayesha", text = "I can help look for the cat near the market.", timestamp = "5m"),
        Comment(id = "c2", postId = "1", author = "Rafi", text = "I saw a similar cat yesterday.", timestamp = "10m")
    )

    NeighborhoodHelperTheme(dynamicColor = false) {
        FeedContent(posts = samplePosts, comments = sampleComments, onWilling = { }, onAddComment = { _, _, _ -> }, onPostClick = { })
    }
}

@Preview(showBackground = true, name = "FeedScreenStyledPreview")
@Composable
fun FeedScreenStyledPreview() {
    val samplePosts = listOf(
        Post(
            id = "1",
            username = "Maishan Nadis",
            userAvatarUrl = "",
            timestamp = "2m",
            content = "Lost cat near Kalabagan Please keep an eye out!",
            imageUrl = null,
            likes = 4,
            comments = 2
        ),
        Post(
            id = "2",
            username = "Faiza Tashmeah",
            userAvatarUrl = "",
            timestamp = "15m",
            content = "Anyone has a charger-fan I can borrow this afternoon?",
            imageUrl = null,
            likes = 1,
            comments = 5
        ),
        Post(
            id = "3",
            username = "Safwat Bushra",
            userAvatarUrl = "",
            timestamp = "1h",
            content = "Need a Math tutor for my cousin. Any recommendations?",
            imageUrl = null,
            likes = 12,
            comments = 3
        )
    )

    NeighborhoodHelperTheme(dynamicColor = false) {
        FeedContent(posts = samplePosts, comments = emptyList(), onWilling = { }, onAddComment = { _, _, _ -> }, onPostClick = { })
    }
}

@Preview(showBackground = true, name = "FeedScreenDeepStyledPreview")
@Composable
fun FeedScreenDeepStyledPreview() {
    val samplePosts = listOf(
        Post(
            id = "1",
            username = "Ayesha Khan",
            userAvatarUrl = "",
            timestamp = "5m",
            content = "Extra groceries available — happy to share!",
            imageUrl = "dummy1",
            likes = 10,
            comments = 7
        ),
        Post(
            id = "2",
            username = "Rafi Ahmed",
            userAvatarUrl = "",
            timestamp = "20m",
            content = "Found a wallet near the park. Describe it to claim.",
            imageUrl = "dummy2",
            likes = 3,
            comments = 1
        ),
        Post(
            id = "3",
            username = "Nadia Rahman",
            userAvatarUrl = "",
            timestamp = "1h",
            content = "Hosting a free tutoring session for middle school math this weekend.",
            imageUrl = "dummy3",
            likes = 12,
            comments = 9
        )
    )

    NeighborhoodHelperTheme(dynamicColor = false) {
        FeedContent(posts = samplePosts, comments = emptyList(), onWilling = { }, onAddComment = { _, _, _ -> }, onPostClick = { })
    }
}

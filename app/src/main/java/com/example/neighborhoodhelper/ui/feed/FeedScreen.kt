@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.neighborhoodhelper.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import androidx.compose.material3.HorizontalDivider

@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value

    FeedContent(posts = posts, onAccept = { viewModel.accept(it) })
}

@Composable
fun FeedContent(
    posts: List<Post>,
    onAccept: (postId: String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Neighborhood Feed",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(posts, key = { _, item -> item.id }) { index, post ->
                PostCard(
                    post = post,
                    onAccept = { onAccept(post.id) }
                )

                if (index < posts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onAccept: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Avatar + Username + Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(username = post.username)
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

            // Content text
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Optional image or placeholder with rounded corners and subtle border
            val imageShape = RoundedCornerShape(12.dp)
            if (!post.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .clip(imageShape)
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(imageShape)
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), shape = imageShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Single Accept action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ElevatedButton(
                    onClick = onAccept,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
                ) {
                    Text("Accept (${post.likes})")
                }
            }
        }
    }
}

@Composable
private fun Avatar(username: String) {
    val sizeDp = 40.dp
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        // Initials placeholder
        val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Text(
            initial,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

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

    NeighborhoodHelperTheme {
        FeedContent(posts = samplePosts, onAccept = {})
    }
}

@Preview(showBackground = true, name = "FeedScreenFullPreview")
@Composable
fun FeedScreenFullPreview() {
    // For preview we avoid constructing a ViewModel. Reuse sample posts to show the full screen.
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

    NeighborhoodHelperTheme {
        FeedContent(posts = samplePosts, onAccept = {})
    }
}

@Preview(showBackground = true, name = "FeedScreenInteractivePreview")
@Composable
fun FeedScreenInteractivePreview() {
    // Interactive preview: likes update when Accept is pressed using local state.
    val sample = listOf(
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

    val postsState = remember { mutableStateListOf<Post>().also { it.addAll(sample) } }

    NeighborhoodHelperTheme {
        FeedContent(posts = postsState, onAccept = { postId ->
            val idx = postsState.indexOfFirst { it.id == postId }
            if (idx >= 0) {
                postsState[idx] = postsState[idx].copy(likes = postsState[idx].likes + 1)
            }
        })
    }
}

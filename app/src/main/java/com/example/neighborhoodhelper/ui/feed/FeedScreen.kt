@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.neighborhoodhelper.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import androidx.navigation.NavController

@Composable
fun FeedScreen(viewModel: FeedViewModel, navController: NavController) {
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value

    FeedContent(
        posts = posts,
        onWilling = { /* no-op */ },
        onPostClick = { id -> navController.navigate("postDetail/$id") }
    )
}

@Composable
fun FeedContent(
    posts: List<Post>,
    onWilling: (postId: String) -> Unit,
    onPostClick: (postId: String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(3.dp),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(posts, key = { _, item -> item.id }) { index, post ->
                PostCard(
                    post = post,
                    onWilling = { onWilling(post.id) },
                    modifier = Modifier.clickable { onPostClick(post.id) }
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
    onWilling: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

            // Optional image or placeholder with rounded corners and subtle elevation
            val imageShape = RoundedCornerShape(12.dp)
            if (!post.imageUrl.isNullOrBlank()) {
                ElevatedCard(
                    shape = imageShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                ElevatedCard(
                    shape = imageShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Actions: Willing (left) + Comment (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onWilling,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
                ) {
                    Text("Willing")
                }

                OutlinedButton(
                    onClick = { /* no-op */ },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "${post.comments}")
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

    NeighborhoodHelperTheme(dynamicColor = false) {
        FeedContent(posts = samplePosts, onWilling = { }, onPostClick = { })
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
        FeedContent(posts = samplePosts, onWilling = { }, onPostClick = { })
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
        FeedContent(posts = samplePosts, onWilling = { }, onPostClick = { })
    }
}

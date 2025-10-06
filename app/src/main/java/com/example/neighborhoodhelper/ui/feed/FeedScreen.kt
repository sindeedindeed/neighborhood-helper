@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.neighborhoodhelper.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme

@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Neighborhood Feed") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onLike = { viewModel.like(post.id) },
                    onComment = { viewModel.comment(post.id) }
                )
            }
        }
    }
}

@Composable
private fun PostCard(
    post: Post,
    onLike: () -> Unit,
    onComment: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                        style = MaterialTheme.typography.titleMedium,
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
                Text(text = post.content, style = MaterialTheme.typography.bodyMedium)
            }

            // Optional image or placeholder
            if (!post.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onLike) {
                    Text("Like (${post.likes})")
                }
                OutlinedButton(onClick = onComment) {
                    Text("Comment (${post.comments})")
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
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Initials placeholder
        val initial = username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        Text(initial, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true, name = "FeedScreenPreview")
@Composable
private fun FeedScreenPreview() {
    NeighborhoodHelperTheme {
        FeedScreen(viewModel = FeedViewModel())
    }
}

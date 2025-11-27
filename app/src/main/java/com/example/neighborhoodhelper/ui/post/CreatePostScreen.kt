// CreatePostScreen.kt - UI for creating a new post with image attachment and urgent flag
package com.example.neighborhoodhelper.ui.post

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val NeutralBackground = Color(0xFFE0E0E0)

@Composable
fun CreatePostScreen(
    viewModel: PostViewModel = viewModel(),
    onPostSubmitted: (PostRecord) -> Unit = {}
) {
    val text by viewModel.text.collectAsState()
    val imageBitmap by viewModel.imageBitmap.collectAsState()
    val isUrgent by viewModel.isUrgent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Gallery launcher to pick image from device storage
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val bmp = android.graphics.BitmapFactory.decodeStream(input)
                input?.close()
                viewModel.setImageBitmap(bmp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera launcher to take a photo
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        viewModel.setImageBitmap(bmp)
    }

    // If loading, show the loading screen instead
    if (isLoading) {
        LoadingScreen(isUrgent = isUrgent)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Header with title and post button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Create Post", style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = {
                    if (text.isNotBlank() || imageBitmap != null) {
                        viewModel.submitPost { result ->
                            result.onSuccess { record -> onPostSubmitted(record) }
                            result.onFailure { }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text(text = if (isLoading) "Posting..." else "Post")
            }
        }

        // Main post card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = if (isUrgent) BorderStroke(2.dp, Color.Red) else BorderStroke(1.dp, NeutralBackground),
            colors = CardDefaults.cardColors(containerColor = NeutralBackground)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // User header with profile picture and name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "MN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Maishan Nadis", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Public",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text input field
                OutlinedTextField(
                    value = text,
                    onValueChange = { viewModel.setText(it) },
                    placeholder = { Text(text = "What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display selected image if available
                imageBitmap?.let { bmp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action buttons and urgent toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Text(text = "Gallery")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { cameraLauncher.launch(null) }) {
                            Text(text = "Camera")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Urgent", modifier = Modifier.padding(end = 8.dp))
                        Switch(checked = isUrgent, onCheckedChange = { viewModel.setUrgent(it) })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Submit button
        Button(
            onClick = {
                if (text.isNotBlank() || imageBitmap != null) {
                    viewModel.submitPost { result ->
                        result.onSuccess { record -> onPostSubmitted(record) }
                        result.onFailure { }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = "Post")
        }
    }
}


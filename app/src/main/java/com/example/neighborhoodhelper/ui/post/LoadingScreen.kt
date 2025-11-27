// LoadingScreen.kt - UI for displaying loading state while searching for nearby helpers
package com.example.neighborhoodhelper.ui.post

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen(
    isUrgent: Boolean
) {
    // Blinking animation for urgent posts
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    val backgroundColor = if (isUrgent) {
        Color.Red.copy(alpha = blinkAlpha)
    } else {
        MaterialTheme.colorScheme.background
    }

    val textColor = if (isUrgent) Color.White else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Spinner indicator
            CircularProgressIndicator(
                color = if (isUrgent) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main message
            Text(
                text = "Searching for nearby helpers…",
                color = textColor,
                style = MaterialTheme.typography.titleMedium
            )

            // Urgent badge if applicable
            if (isUrgent) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small)
                        .padding(8.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "⚠ URGENT REQUEST",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Subtext
            Text(
                text = "We are notifying nearby helpers about your request",
                color = textColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


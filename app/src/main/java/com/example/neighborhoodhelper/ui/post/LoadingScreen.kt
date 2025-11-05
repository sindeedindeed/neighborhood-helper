// LoadingScreen.kt
// Compose screen shown after submitting a post while searching for nearby helpers. Shows a progress indicator
// and applies a red blinking background when the request is marked as urgent.

package com.example.neighborhoodhelper.ui.post

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen(
    isUrgent: Boolean
) {
    val infinite = rememberInfiniteTransition()
    val blinkAlpha = if (isUrgent) {
        infinite.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isUrgent) Color.Red.copy(alpha = blinkAlpha) else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = if (isUrgent) Color.White else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Searching for nearby helpers…",
                color = if (isUrgent) Color.White else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            if (isUrgent) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "URGENT REQUEST", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

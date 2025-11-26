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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun LoadingScreen(
    isUrgent: Boolean,
    postId: String? = null,
    onAssigned: ((helperId: String) -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition()
    val blinkAlpha = if (isUrgent) {
        infinite.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        ).value
    } else {
        1f
    }

    // Firestore listener for assignment
    LaunchedEffect(postId) {
        if (postId != null && onAssigned != null) {
            val docRef = Firebase.firestore.collection("posts").document(postId)
            val subscription = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val assigned = snapshot.getString("assignedHelperId")
                    if (!assigned.isNullOrBlank()) {
                        onAssigned(assigned)
                    }
                }
            }
            // remove listener when composable leaves scope
            awaitDispose { subscription.remove() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isUrgent) Color.Red.copy(alpha = blinkAlpha) else MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            CircularProgressIndicator(
                color = if (isUrgent) Color.White else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Searching for nearby helpers…",
                color = if (isUrgent) Color.White else MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )

            if (isUrgent) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "URGENT REQUEST",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

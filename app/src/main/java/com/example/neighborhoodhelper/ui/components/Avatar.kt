package com.example.neighborhoodhelper.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun Avatar(url: String?, modifier: Modifier = Modifier) {
    Surface(shape = CircleShape, tonalElevation = 2.dp, modifier = modifier.size(48.dp)) {
        if (!url.isNullOrEmpty()) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Icon(imageVector = Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}


package com.example.neighborhoodhelper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TaskStatusBadge(status: String) {
    val (backgroundColor, textColor, displayText) = when (status) {
        "active" -> Triple(Color(0xFF4CAF50), Color.White, "Active")
        "matched" -> Triple(Color(0xFF2196F3), Color.White, "Matched")
        "in_progress" -> Triple(Color(0xFFFFC107), Color(0xFF424242), "In Progress")
        "completed" -> Triple(Color(0xFF9E9E9E), Color.White, "Completed")
        "cancelled" -> Triple(Color(0xFFF44336), Color.White, "Cancelled")
        else -> Triple(Color(0xFFE0E0E0), Color(0xFF757575), "Unknown")
    }

    Text(
        text = displayText,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
    )
}


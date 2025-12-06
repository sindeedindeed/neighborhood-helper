package com.example.neighborhoodhelper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun RatingDialog(
    userName: String,
    onDismiss: () -> Unit,
    onSubmit: (rating: Float, review: String) -> Unit
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var review by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Rate $userName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C63FF)
                )

                Text(
                    text = "How was your experience?",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                // Star Rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Star $i",
                            tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { rating = i.toFloat() }
                        )
                    }
                }

                if (rating > 0) {
                    Text(
                        text = when {
                            rating <= 2 -> "We're sorry to hear that"
                            rating <= 3 -> "Good experience"
                            rating <= 4 -> "Great experience!"
                            else -> "Excellent experience!"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Review TextField
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("Write a review (optional)") },
                    placeholder = { Text("Share your experience...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (rating > 0) {
                                onSubmit(rating, review)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = rating > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF)
                        )
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}


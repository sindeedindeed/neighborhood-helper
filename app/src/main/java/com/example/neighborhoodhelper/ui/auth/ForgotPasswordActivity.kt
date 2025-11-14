package com.example.neighborhoodhelper.ui.auth
import com.google.android.gms.tasks.Task

import android.os.Bundle
import android.util.Log
import android.util.Patterns // Import Patterns for email validation
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth // Import Firebase Authentication

class ForgotPasswordActivity : ComponentActivity() {

    private lateinit var firebaseAuth: FirebaseAuth // Declare FirebaseAuth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance() // Initialize FirebaseAuth
        val initialEmail = intent.getStringExtra("username") ?: "" // Changed to initialEmail for clarity

        setContent {
            MaterialTheme {
                ForgotPasswordScreen(
                    initialEmail = initialEmail, // Pass initial email
                    firebaseAuth = firebaseAuth, // Pass FirebaseAuth instance
                    onBackClick = { finish() },
                    onSuccess = {
                        Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    initialEmail: String, // Renamed for clarity
    firebaseAuth: FirebaseAuth, // Receive FirebaseAuth instance
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf(initialEmail) } // Renamed for clarity
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Helper function for email validation
    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Text(
                text = "🔐",
                fontSize = 60.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Forgot Password?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            Text(
                text = "Enter your email to receive a reset link.",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            errorMessage = "" // Clear error on input change
                        },
                        label = { Text("Email Address") },
                        placeholder = { Text("Enter your email address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        isError = emailInput.isNotBlank() && !isValidEmail(emailInput), // Show error if invalid email
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailInput.isNotBlank() && !isValidEmail(emailInput)) Color.Red else Color(0xFF6C63FF),
                            unfocusedBorderColor = if (emailInput.isNotBlank() && !isValidEmail(emailInput)) Color.Red else Color(0xFFE0E0E0)
                        )
                    )

                    // Display email validation error
                    if (emailInput.isNotBlank() && !isValidEmail(emailInput)) {
                        Text(
                            text = "Please enter a valid email address.",
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    } else if (errorMessage.isNotEmpty()) { // Display other errors
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFEBEE)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage,
                                    color = Color(0xFFC62828),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Send Reset Link Button
                    Button(
                        onClick = {
                            if (!isValidEmail(emailInput)) {
                                errorMessage = "Please enter a valid email address."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = ""

                            firebaseAuth.sendPasswordResetEmail(emailInput)
                                .addOnCompleteListener { task: Task<Void> -> // <--- CHANGE IS HERE
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Log.d("ForgotPassword", "Password reset email sent to: $emailInput")
                                        onSuccess() // Indicate success and finish activity
                                    } else {
                                        val exceptionMessage = task.exception?.message ?: "Unknown error"
                                        errorMessage = "Failed to send reset email: $exceptionMessage"
                                        Log.e("ForgotPassword", "Failed to send reset email: $exceptionMessage", task.exception)
                                        Toast.makeText(
                                            context,
                                            "Error: $exceptionMessage",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isLoading && emailInput.isNotBlank() && isValidEmail(emailInput),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Send Reset Link",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Back to Sign In
            TextButton(onClick = onBackClick) {
                Text(
                    text = "← Back to Sign In",
                    color = Color(0xFF6C63FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

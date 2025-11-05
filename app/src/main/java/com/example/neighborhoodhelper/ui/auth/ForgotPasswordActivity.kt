package com.example.neighborhoodhelper.ui.auth

import android.os.Bundle
import android.util.Log
import android.util.Patterns
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.FirebaseAuth // Import Firebase Authentication

class ForgotPasswordActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth // Declare FirebaseAuth instance

    companion object;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance() // Initialize FirebaseAuth
        val username = intent.getStringExtra("username") ?: ""

        setContent {
            MaterialTheme {
                ForgotPasswordScreen(
                    initialUsername = username,
                    firestore = firestore,
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

data class UserRecoveryData(
    val maskedEmail: String,
    val maskedPhone: String,
    val fullEmail: String,
    val fullPhone: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    initialUsername: String,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth, // Receive FirebaseAuth instance
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var username by remember { mutableStateOf(initialUsername) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var userData by remember { mutableStateOf<UserRecoveryData?>(null) }

    val context = LocalContext.current

    // Function to fetch user data from Firestore
    fun fetchUserData() {
        if (username.isBlank()) {
            errorMessage = "Please enter username or email"
            return
        }

        isLoading = true
        errorMessage = ""

        // Use lifecycleScope from ComponentActivity
        (context as? ComponentActivity)?.lifecycleScope?.launch {
            try {
                // Check if input is email or username
                val isEmail = Patterns.EMAIL_ADDRESS.matcher(username).matches()

                val query = if (isEmail) {
                    firestore.collection("users")
                        .whereEqualTo("email", username)
                } else {
                    firestore.collection("users")
                        .whereEqualTo("username", username)
                }

                val documents = query.get().await()

                if (!documents.isEmpty) {
                    val userDoc = documents.documents[0]
                    val email = userDoc.getString("email") ?: ""
                    val phone = userDoc.getString("phone") ?: ""

                    if (email.isEmpty()) {
                        errorMessage = "No email found for this account"
                        isLoading = false
                        return@launch
                    }

                    // Create masked versions
                    userData = UserRecoveryData(
                        maskedEmail = maskEmail(email),
                        maskedPhone = maskPhone(phone),
                        fullEmail = email,
                        fullPhone = phone
                    )

                    Log.d("ForgotPassword", "User data fetched successfully from Firestore for: $email")
                } else {
                    errorMessage = "User not found in Firestore. Please check username or email."
                    Log.e("ForgotPassword", "User not found in Firestore for: $username")
                }

                isLoading = false
            } catch (e: Exception) {
                Log.e("ForgotPassword", "Error fetching user data from Firestore: ${e.message}", e)
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
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
                text = "Enter your username or email to recover",
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

                    // If no user data yet, show input field
                    if (userData == null) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = ""
                            },
                            label = { Text("Username or Email") },
                            placeholder = { Text("Enter username or email") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6C63FF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )

                        // Error message
                        if (errorMessage.isNotEmpty()) {
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

                        // Continue Button
                        Button(
                            onClick = { fetchUserData() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = !isLoading && username.isNotBlank(),
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
                                    text = "Continue",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Show user data (masked email and phone)
                        Text(
                            text = "Recovery Options",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Email option
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📧 Email",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = userData!!.maskedEmail,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Phone option (if available)
                        if (userData!!.fullPhone.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF5F5F5)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📱 Phone",
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = userData!!.maskedPhone,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Send Reset Link Button
                        Button(
                            onClick = {
                                val emailToReset = userData!!.fullEmail
                                isLoading = true
                                errorMessage = ""

                                firebaseAuth.sendPasswordResetEmail(emailToReset)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            Log.d("ForgotPassword", "Password reset email sent to: $emailToReset")
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
                            enabled = !isLoading,
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Try Different Account
                        TextButton(
                            onClick = {
                                userData = null
                                username = initialUsername // Clear input for new attempt
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Try Different Account",
                                color = Color(0xFF6C63FF)
                            )
                        }
                        // Display error message if present after attempting to send reset email
                        if (errorMessage.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
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

// Helper function to mask email
private fun maskEmail(email: String): String {
    if (email.isEmpty()) return ""

    val parts = email.split("@")
    if (parts.size != 2) return email

    val username = parts[0]
    val domain = parts[1]

    return when {
        username.length <= 2 -> "${username[0]}***@$domain"
        else -> "${username[0]}${"*".repeat(username.length - 2)}${username.last()}@$domain"
    }
}

// Helper function to mask phone
private fun maskPhone(phone: String): String {
    if (phone.isEmpty()) return ""

    return when {
        phone.length <= 4 -> phone
        phone.startsWith("+") -> {
            val countryCode = phone.take(4) // e.g., +880
            val lastDigits = phone.takeLast(2)
            "$countryCode *******$lastDigits"
        }
        else -> {
            val lastDigits = phone.takeLast(2)
            "*******$lastDigits"
        }
    }
}
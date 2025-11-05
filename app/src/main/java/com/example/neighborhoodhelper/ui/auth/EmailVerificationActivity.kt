package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import android.util.Log

class EmailVerificationActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val userEmail = intent.getStringExtra("user_email") ?: ""
        val userId = intent.getStringExtra("user_id") ?: ""

        setContent {
            EmailVerificationScreen(
                userEmail = userEmail,
                userId = userId,
                auth = auth,
                firestore = firestore,
                onVerificationComplete = {
                    // Navigate to main activity or sign in
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    userEmail: String,
    userId: String,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    onVerificationComplete: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var autoCheckEnabled by remember { mutableStateOf(true) }

    // Auto-check for verification every 3 seconds
    LaunchedEffect(autoCheckEnabled) {
        while (autoCheckEnabled) {
            delay(3000)
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isVerified = auth.currentUser?.isEmailVerified ?: false
                    Log.d("EmailVerification", "Checking verification status: $isVerified")

                    if (isVerified) {
                        // Update Firestore
                        firestore.collection("users")
                            .document(userId)
                            .update("emailVerified", true)
                            .addOnSuccessListener {
                                Log.d("EmailVerification", "Firestore updated successfully")
                                autoCheckEnabled = false
                                onVerificationComplete()
                            }
                            .addOnFailureListener { e ->
                                Log.e("EmailVerification", "Failed to update Firestore: ${e.message}")
                            }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Email Icon
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = "Email",
            tint = Color(0xFF6C63FF),
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp)
        )

        // Title
        Text(
            text = "Verify Your Email",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Email address
        Text(
            text = userEmail,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6C63FF),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Description
        Text(
            text = "We've sent a verification link to your email address. Please click the link to verify your account.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Auto-checking indicator
        if (autoCheckEnabled) {
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF6C63FF),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Checking verification status...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Manual Check Button
        Button(
            onClick = {
                isChecking = true
                auth.currentUser?.reload()?.addOnCompleteListener { task ->
                    isChecking = false
                    if (task.isSuccessful) {
                        val isVerified = auth.currentUser?.isEmailVerified ?: false

                        if (isVerified) {
                            // Update Firestore
                            firestore.collection("users")
                                .document(userId)
                                .update("emailVerified", true)
                                .addOnSuccessListener {
                                    autoCheckEnabled = false
                                    onVerificationComplete()
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "Email not verified yet. Please check your inbox.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to check verification status",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isChecking && !isResending,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6C63FF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Check",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I've Verified My Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resend Email Button
        OutlinedButton(
            onClick = {
                isResending = true
                auth.currentUser?.sendEmailVerification()
                    ?.addOnCompleteListener { task ->
                        isResending = false
                        if (task.isSuccessful) {
                            Toast.makeText(
                                context,
                                "Verification email resent!",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to resend email: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isChecking && !isResending,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6C63FF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF6C63FF),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Resend Verification Email",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Out Button
        TextButton(
            onClick = {
                auth.signOut()
                val intent = Intent(context, SignInActivity::class.java)
                context.startActivity(intent)
                if (context is ComponentActivity) {
                    context.finish()
                }
            }
        ) {
            Text(
                text = "Back to Sign In",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
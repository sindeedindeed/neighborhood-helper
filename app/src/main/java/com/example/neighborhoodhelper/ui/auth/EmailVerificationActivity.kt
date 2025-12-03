package com.example.neighborhoodhelper.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth // Needed if you want to resend email from here

class EmailVerificationSentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EmailVerificationSentScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationSentScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // TOP BAR WITH BACK BUTTON AND TITLE (Fixed - Not scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // Navigate back to SignInActivity
                    val intent = Intent(context, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Sign In",
                    tint = Color.Black
                )
            }

            Text(
                text = "Verify Your Email",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📧", // Email emoji
                fontSize = 80.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "Verification Email Sent!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "We've sent a verification link to your email address. Please check your inbox (and spam folder!) to activate your account. You can sign in once your email is verified.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Button to go to Sign In page
            Button(
                onClick = {
                    val intent = Intent(context, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    context.startActivity(intent)
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Go to Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            // Optional: Resend Email Button (if you want to add this functionality here)
            // You would need to add FirebaseAuth.getInstance() and call currentUser?.sendEmailVerification()
            // Spacer(modifier = Modifier.height(16.dp))
            // Text(
            //     text = "Didn't receive the email? Resend",
            //     fontSize = 14.sp,
            //     color = Color(0xFF6C63FF),
            //     fontWeight = FontWeight.Medium,
            //     modifier = Modifier.clickable {
            //         // Logic to resend email
            //         FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
            //             ?.addOnCompleteListener { task ->
            //                 if (task.isSuccessful) {
            //                     Toast.makeText(context, "Verification email re-sent!", Toast.LENGTH_SHORT).show()
            //                 } else {
            //                     Toast.makeText(context, "Failed to re-send email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            //                 }
            //             }
            //     }
            // )
        }
    }
}

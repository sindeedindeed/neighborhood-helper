package com.example.neighborhoodhelper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.neighborhoodhelper.ui.auth.LandingActivity
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import com.google.firebase.FirebaseApp  // ✅ Changed import
import com.google.firebase.auth.FirebaseAuth  // ✅ Changed import

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth (Non-KTX way)
        try {
            // ✅ Initialize Firebase App first
            FirebaseApp.initializeApp(this)

            // ✅ Get Firebase Auth instance
            auth = FirebaseAuth.getInstance()

            // ✅ TEST 1: Firebase Auth Initialization
            Log.d(TAG, "╔═══════════════════════════════════════╗")
            Log.d(TAG, "║   FIREBASE CONNECTION TEST            ║")
            Log.d(TAG, "╚═══════════════════════════════════════╝")
            Log.d(TAG, "✅ Firebase Auth initialized successfully")
            Log.d(TAG, "📦 Auth instance: ${auth.javaClass.simpleName}")

            // ✅ TEST 2: Check current user
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "👤 Current User Info:")
                Log.d(TAG, "   ├─ Email: ${currentUser.email}")
                Log.d(TAG, "   ├─ UID: ${currentUser.uid}")
                Log.d(TAG, "   ├─ Display Name: ${currentUser.displayName ?: "Not set"}")
                Log.d(TAG, "   ├─ Email Verified: ${currentUser.isEmailVerified}")
                Log.d(TAG, "   ├─ Provider: ${currentUser.providerId}")
                Log.d(TAG, "   └─ Photo URL: ${currentUser.photoUrl ?: "None"}")
            } else {
                Log.d(TAG, "👤 No user currently signed in")
            }

            // ✅ TEST 3: Firebase App Configuration
            val firebaseApp = FirebaseApp.getInstance()
            Log.d(TAG, "🔧 Firebase Configuration:")
            Log.d(TAG, "   ├─ App Name: ${firebaseApp.name}")
            Log.d(TAG, "   ├─ Package Name: ${packageName}")
            Log.d(TAG, "   └─ Project ID: ${firebaseApp.options.projectId}")

            Log.d(TAG, "════════════════════════════════════════")

            Toast.makeText(
                this,
                "✅ Firebase Connected! Check Logcat",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
            Toast.makeText(
                this,
                "❌ Firebase Error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()

            redirectToLanding()
            return
        }

        enableEdgeToEdge()
        setContent {
            NeighborhoodHelperTheme {
                BackHandler {
                    moveTaskToBack(true)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Neighborhood Helper") },
                            actions = {
                                IconButton(onClick = { signOut() }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Sign Out"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color(0xFF6C63FF),
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White
                            )
                        )
                    }
                ) { innerPadding ->
                    MainContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Log.d(TAG, "📱 onStart() called")

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.d(TAG, "🔐 No user signed in - Redirecting to Landing")
            redirectToLanding()
        } else {
            Log.d(TAG, "👤 User is signed in: ${currentUser.email}")
        }
    }

    private fun redirectToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun signOut() {
        Log.d(TAG, "🚪 Sign out initiated")

        try {
            auth.signOut()
            Log.d(TAG, "✅ Sign out successful")
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            redirectToLanding()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign out failed", e)
            Toast.makeText(this, "Sign out failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun MainContent(modifier: Modifier = Modifier) {
    // ✅ Use getInstance() instead of Firebase.auth
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Firebase Connection Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✅",
                    fontSize = 32.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Column {
                    Text(
                        text = "Firebase Connected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Authentication is active",
                        fontSize = 12.sp,
                        color = Color(0xFF558B2F)
                    )
                }
            }
        }

        // User Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    color = Color(0xFF6C63FF)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentUser?.displayName ?: "User",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentUser?.email ?: "No email",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "UID", value = currentUser?.uid?.take(20)?.plus("...") ?: "N/A")
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Email Verified",
                    value = if (currentUser?.isEmailVerified == true) "✅ Yes" else "❌ No"
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Provider",
                    value = currentUser?.providerData?.firstOrNull()?.providerId ?: "Unknown"
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Created",
                    value = currentUser?.metadata?.creationTimestamp?.let {
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(it))
                    } ?: "Unknown"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Neighborhood Helper! 🎉",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6C63FF)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You're successfully signed in with Firebase Authentication.",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    NeighborhoodHelperTheme {
        MainContent()
    }
}
package com.example.neighborhoodhelper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.neighborhoodhelper.auth.AuthManager
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.ui.auth.LandingActivity
import com.example.neighborhoodhelper.ui.chat.ChatListScreen
import com.example.neighborhoodhelper.ui.chat.ChatScreen
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.profile.ProfileSetupScreen
import com.example.neighborhoodhelper.ui.settings.SettingsScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val authManager = AuthManager()
    private val repository = FirebaseRepository()

    companion object {
        private const val TAG = "MainActivity"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Notification permission granted")
            getFCMToken()
        } else {
            Log.d(TAG, "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        try {
            FirebaseApp.initializeApp(this)
            auth = FirebaseAuth.getInstance()

            Log.d(TAG, "╔═══════════════════════════════════════╗")
            Log.d(TAG, "║   FIREBASE CONNECTION TEST            ║")
            Log.d(TAG, "╚═══════════════════════════════════════╝")
            Log.d(TAG, "✅ Firebase Auth initialized successfully")
            Log.d(TAG, "📦 Auth instance: ${auth.javaClass.simpleName}")

            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "👤 Current User Info:")
                Log.d(TAG, "   ├─ Email: ${currentUser.email}")
                Log.d(TAG, "   ├─ UID: ${currentUser.uid}")
                Log.d(TAG, "   ├─ Display Name: ${currentUser.displayName ?: "Not set"}")
                Log.d(TAG, "   ├─ Email Verified: ${currentUser.isEmailVerified}")
                Log.d(TAG, "   └─ Provider: ${currentUser.providerId}")
            } else {
                Log.d(TAG, "👤 No user currently signed in")
            }

            val firebaseApp = FirebaseApp.getInstance()
            Log.d(TAG, "🔧 Firebase Configuration:")
            Log.d(TAG, "   ├─ App Name: ${firebaseApp.name}")
            Log.d(TAG, "   ├─ Package Name: ${packageName}")
            Log.d(TAG, "   └─ Project ID: ${firebaseApp.options.projectId}")
            Log.d(TAG, "════════════════════════════════════════")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
            Toast.makeText(this, "❌ Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
            redirectToLanding()
            return
        }

        enableEdgeToEdge()

        // Request notification permission for Android 13+
        requestNotificationPermission()

        setContent {
            NeighborhoodHelperTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticating by remember { mutableStateOf(true) }
                    var authError by remember { mutableStateOf<String?>(null) }
                    var startDestination by remember { mutableStateOf("feed") }

                    // Authenticate on app start
                    LaunchedEffect(Unit) {
                        lifecycleScope.launch {
                            // Check if user is already signed in with email/password
                            val currentUser = auth.currentUser
                            if (currentUser != null && !currentUser.isAnonymous) {
                                // User is signed in with email/password
                                Log.d(TAG, "User already signed in: ${currentUser.email}")

                                // Check if user has completed profile
                                val hasProfile = repository.hasCompletedProfile()
                                startDestination = if (hasProfile) "feed" else "profile_setup"

                                getFCMToken()
                            } else {
                                // Try anonymous sign-in for guest users
                                val result = authManager.signInAnonymouslyIfNeeded()
                                if (result.isFailure) {
                                    authError = result.exceptionOrNull()?.message
                                } else {
                                    val hasProfile = repository.hasCompletedProfile()
                                    startDestination = if (hasProfile) "feed" else "profile_setup"
                                    getFCMToken()
                                }
                            }
                            isAuthenticating = false
                        }
                    }

                    when {
                        isAuthenticating -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        authError != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Authentication Error: $authError",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            val navController = rememberNavController()

                            BackHandler {
                                // Handle back button to prevent accidental exits
                                if (navController.currentDestination?.route == "feed") {
                                    moveTaskToBack(true)
                                } else {
                                    navController.navigateUp()
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = startDestination
                            ) {
                                composable("profile_setup") {
                                    ProfileSetupScreen(navController = navController)
                                }

                                composable("feed") {
                                    FeedScreen(navController = navController)
                                }

                                composable(
                                    route = "postDetail/{postId}",
                                    arguments = listOf(navArgument("postId") { type = NavType.StringType })
                                ) { backStackEntry ->
                                    val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                    PostDetailScreen(
                                        navController = navController,
                                        postId = postId
                                    )
                                }

                                composable("chatList") {
                                    ChatListScreen(
                                        onBack = { navController.navigateUp() },
                                        onChatRoomClick = { roomId ->
                                            navController.navigate("chat/$roomId/User")
                                        }
                                    )
                                }

                                composable(
                                    route = "chat/{roomId}/{otherUserName}",
                                    arguments = listOf(
                                        navArgument("roomId") { type = NavType.StringType },
                                        navArgument("otherUserName") { type = NavType.StringType }
                                    )
                                ) { backStackEntry ->
                                    val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                                    val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "User"
                                    ChatScreen(
                                        roomId = roomId,
                                        otherUserName = otherUserName,
                                        onBack = { navController.navigateUp() }
                                    )
                                }

                                composable("settings") {
                                    SettingsScreen(
                                        onBack = { navController.navigateUp() }
                                    )
                                }
                            }
                        }
                    }
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
            Log.d(TAG, "👤 User is signed in: ${currentUser.email ?: "Anonymous"}")
        }
    }

    private fun redirectToLanding() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Notification permission already granted")
                    getFCMToken()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, permission is granted by default
            getFCMToken()
        }
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            // Update token in Firestore
            lifecycleScope.launch {
                repository.updateFcmToken(token)
            }
        }
    }
}
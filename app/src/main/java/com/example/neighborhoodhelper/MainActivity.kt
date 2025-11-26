package com.example.neighborhoodhelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
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
import com.example.neighborhoodhelper.ui.chat.ChatListScreen
import com.example.neighborhoodhelper.ui.chat.ChatScreen
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.profile.ProfileSetupScreen
import com.example.neighborhoodhelper.ui.settings.SettingsScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val authManager = AuthManager()
    private val repository = FirebaseRepository()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
            getFCMToken()
        } else {
            Log.d("MainActivity", "Notification permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            val result = authManager.signInAnonymouslyIfNeeded()
                            if (result.isFailure) {
                                authError = result.exceptionOrNull()?.message
                            } else {
                                // Check if user has completed profile
                                val hasProfile = repository.hasCompletedProfile()
                                startDestination = if (hasProfile) "feed" else "profile_setup"

                                // Get FCM token after authentication
                                getFCMToken()
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
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
                Log.w("MainActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("MainActivity", "FCM Token: $token")

            // Update token in Firestore
            lifecycleScope.launch {
                repository.updateFcmToken(token)
            }
        }
    }
}
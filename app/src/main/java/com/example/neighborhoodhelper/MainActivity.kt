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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.map.LiveLocationScreen
import com.example.neighborhoodhelper.ui.match.SuccessScreen
import com.example.neighborhoodhelper.ui.notifications.NotificationsScreen
import com.example.neighborhoodhelper.ui.profile.ProfileSetupScreen
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

            Log.d(TAG, "✅ Firebase Auth initialized successfully")

            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d(TAG, "👤 Current User: ${currentUser.email}")
            } else {
                Log.d(TAG, "👤 No user currently signed in")
            }

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
                            val currentUser = auth.currentUser
                            if (currentUser != null && !currentUser.isAnonymous) {
                                Log.d(TAG, "User already signed in: ${currentUser.email}")

                                val hasProfile = repository.hasCompletedProfile()
                                startDestination = if (hasProfile) "feed" else "profile_setup"

                                getFCMToken()
                            } else {
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
                                Text(
                                    text = "Authentication Error: $authError",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            val navController = rememberNavController()
                            val context = LocalContext.current

                            BackHandler {
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

                                composable("notifications") {
                                    NotificationsScreen(
                                        notifications = repository.observeNotifications(),
                                        onNotificationClick = { postId: String ->
                                            navController.navigate("postDetail/$postId")
                                        },
                                        onMarkAsRead = { notificationId: String ->
                                            lifecycleScope.launch {
                                                repository.markNotificationAsRead(notificationId)
                                            }
                                        },
                                        onMarkAllAsViewed = {
                                            lifecycleScope.launch {
                                                repository.markAllNotificationsAsViewed()
                                            }
                                        },
                                        onAcceptWilling = { notificationId: String, postId: String, willingUserId: String ->
                                            lifecycleScope.launch {
                                                val result = repository.acceptWillingUserRequest(
                                                    notificationId,
                                                    postId,
                                                    willingUserId
                                                )
                                                result.onSuccess { willingUser ->
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "✅ Accepted ${willingUser.userName}'s offer",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    navController.navigate("success")
                                                }.onFailure { error ->
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ Error: ${error.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onRejectWilling = { notificationId: String, postId: String, willingUserId: String ->
                                            lifecycleScope.launch {
                                                val result = repository.rejectWillingUserRequest(
                                                    notificationId,
                                                    postId,
                                                    willingUserId
                                                )
                                                result.onSuccess {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Offer declined",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }.onFailure { error ->
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "❌ Error: ${error.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onBack = { navController.popBackStack() }
                                    )
                                }

                                composable("success") {
                                    SuccessScreen(
                                        context = context,
                                        requesterName = "Mr. Person 1",
                                        requesterAddress = "Mirpur DOHS Shopping Mall, Dhaka",
                                        requesterLat = 23.837971826921812,
                                        requesterLon = 90.37527760202093,
                                        onNavigateToMap = { navController.navigate("map") }
                                    )
                                }

                                composable("map") {
                                    LiveLocationScreen(
                                        context = context,
                                        lat = 23.837971826921812,
                                        lon = 90.37527760202093,
                                        markerTitle = "Requester",
                                        onBack = { navController.popBackStack() }
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

            lifecycleScope.launch {
                repository.updateFcmToken(token)
            }
        }
    }
}

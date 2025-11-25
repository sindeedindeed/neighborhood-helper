package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neighborhoodhelper.auth.AuthManager
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.profile.ProfileSetupScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authManager = AuthManager()
    private val repository = FirebaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                                composable("postDetail/{postId}") { backStackEntry ->
                                    val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                    PostDetailScreen(
                                        navController = navController,
                                        postId = postId
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

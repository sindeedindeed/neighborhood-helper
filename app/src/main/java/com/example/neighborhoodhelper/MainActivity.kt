package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeighborhoodHelperTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "feed"
                    ) {
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
package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeighborhoodHelperTheme(dynamicColor = false) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "feed") {
                    composable(route = "feed") {
                        FeedScreen(navController = navController)
                    }
                    composable(
                        route = "postDetail/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: ""
                        PostDetailScreen(navController = navController, postId = postId)
                    }
                }
            }
        }
    }
}

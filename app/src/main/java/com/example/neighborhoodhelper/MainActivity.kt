package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.neighborhoodhelper.ui.details.PostDetailScreen
import com.example.neighborhoodhelper.ui.feed.FeedScreen
import com.example.neighborhoodhelper.ui.feed.FeedViewModel
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Use our deep vibrant palette consistently by disabling dynamic color
            NeighborhoodHelperTheme(dynamicColor = false) {
                val vm: FeedViewModel = viewModel()
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "feed") {
                    composable(route = "feed") {
                        FeedScreen(viewModel = vm, navController = navController)
                    }
                    composable(
                        route = "postDetail/{postId}",
                        arguments = listOf(navArgument("postId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                        PostDetailScreen(postId = postId, onBack = { navController.navigateUp() }, viewModel = vm)
                    }
                }
            }
        }
    }
}

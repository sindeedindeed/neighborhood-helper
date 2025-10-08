package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.neighborhoodhelper.ui.map.LiveLocationScreen
import com.example.neighborhoodhelper.ui.match.SuccessScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeighborhoodHelperTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                NavHost(navController = navController, startDestination = "success") {

                    // Success screen with embedded map
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

                    // Full Live Location screen
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

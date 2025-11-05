package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.neighborhoodhelper.ui.post.CreatePostScreen
import com.example.neighborhoodhelper.ui.post.LoadingScreen
import com.example.neighborhoodhelper.ui.post.PostData
import com.example.neighborhoodhelper.ui.post.PostViewModel
import com.example.neighborhoodhelper.ui.post.SuccessPostScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeighborhoodHelperTheme {
                // Simple screen manager state
                var screen by remember { mutableStateOf<Screen>(Screen.Create) }
                var lastPostData by remember { mutableStateOf<PostData?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (val s = screen) {
                        is Screen.Create -> {
                            val vm = remember { PostViewModel() }
                            CreatePostScreen(viewModel = vm, onPostSubmitted = { postData ->
                                lastPostData = postData
                                screen = Screen.Loading
                            })
                        }

                        is Screen.Loading -> {
                            val urgent = lastPostData?.isUrgent ?: false
                            LoadingScreen(isUrgent = urgent)

                            // Simulate searching and then go to success screen
                            LaunchedEffect(lastPostData) {
                                delay(2000)
                                screen = Screen.Success
                            }
                        }

                        is Screen.Success -> {
                            SuccessPostScreen()
                        }
                    }
                }
            }
        }
    }
}

sealed interface Screen {
    object Create : Screen
    object Loading : Screen
    object Success : Screen
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NeighborhoodHelperTheme {
        SuccessPostScreen()
    }
}
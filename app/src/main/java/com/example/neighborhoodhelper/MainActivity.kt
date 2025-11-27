package com.example.neighborhoodhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neighborhoodhelper.ui.post.CreatePostScreen
import com.example.neighborhoodhelper.ui.post.LoadingScreen
import com.example.neighborhoodhelper.ui.post.PostRecord
import com.example.neighborhoodhelper.ui.post.PostViewModel
import com.example.neighborhoodhelper.ui.post.SuccessPostScreen
import com.example.neighborhoodhelper.ui.theme.NeighborhoodHelperTheme
import kotlinx.coroutines.delay

// Screen navigation states
sealed interface Screen {
    object Create : Screen
    object Loading : Screen
    object Success : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeighborhoodHelperTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Create) }
                var lastPostRecord by remember { mutableStateOf<PostRecord?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)) {

                        when (val s = screen) {
                            is Screen.Create -> {
                                val vm: PostViewModel = viewModel()
                                CreatePostScreen(viewModel = vm, onPostSubmitted = { record ->
                                    lastPostRecord = record
                                    screen = Screen.Loading
                                })
                            }

                            is Screen.Loading -> {
                                val urgent = lastPostRecord?.isUrgent ?: false
                                LoadingScreen(isUrgent = urgent, postId = lastPostRecord?.id, onAssigned = { helperId ->
                                    // handle assignment (navigate to success or details)
                                    screen = Screen.Success
                                })

                                LaunchedEffect(lastPostRecord) {
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
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NeighborhoodHelperTheme {
        SuccessPostScreen()
    }
}
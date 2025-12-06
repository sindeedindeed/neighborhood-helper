package com.example.neighborhoodhelper.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neighborhoodhelper.model.TaskCategory
import com.example.neighborhoodhelper.model.TaskHistory
import com.example.neighborhoodhelper.ui.components.RatingDialog
import com.example.neighborhoodhelper.ui.components.UserRatingDisplay
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskHistoryScreen(
    onBack: () -> Unit
) {
    val viewModel: TaskHistoryViewModel = viewModel()
    val taskHistory by viewModel.taskHistory.collectAsStateWithLifecycle()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var selectedTab by remember { mutableIntStateOf(0) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var ratingTaskHistory by remember { mutableStateOf<TaskHistory?>(null) }

    if (showRatingDialog && ratingTaskHistory != null) {
        val task = ratingTaskHistory!!
        val isRequester = task.requesterId == currentUserId
        val otherUserName = if (isRequester) task.helperName else task.requesterName

        RatingDialog(
            userName = otherUserName,
            onDismiss = {
                showRatingDialog = false
                ratingTaskHistory = null
            },
            onSubmit = { rating, review ->
                viewModel.submitRating(task, rating, review, isRequester)
                showRatingDialog = false
                ratingTaskHistory = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task History", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6C63FF)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "Completed",
                        value = taskHistory.count { it.status == "completed" }.toString()
                    )
                    StatItem(
                        label = "As Requester",
                        value = taskHistory.count { it.requesterId == currentUserId }.toString()
                    )
                    StatItem(
                        label = "As Helper",
                        value = taskHistory.count { it.helperId == currentUserId }.toString()
                    )
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF6C63FF)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All Tasks") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Requested") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Helped") }
                )
            }

            // Task List
            val filteredHistory = when (selectedTab) {
                1 -> taskHistory.filter { it.requesterId == currentUserId }
                2 -> taskHistory.filter { it.helperId == currentUserId }
                else -> taskHistory
            }

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredHistory) { task ->
                        TaskHistoryCard(
                            task = task,
                            currentUserId = currentUserId,
                            onRateClick = {
                                ratingTaskHistory = task
                                showRatingDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6C63FF)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun TaskHistoryCard(
    task: TaskHistory,
    currentUserId: String?,
    onRateClick: () -> Unit
) {
    val isRequester = task.requesterId == currentUserId
    val otherUserName = if (isRequester) task.helperName else task.requesterName
    val myRating = if (isRequester) task.requesterRating else task.helperRating
    val theirRating = if (isRequester) task.helperRating else task.requesterRating
    val category = TaskCategory.fromString(task.category)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(category.color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.displayName,
                            tint = category.color,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = category.displayName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF424242)
                    )
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = task.postContent,
                fontSize = 14.sp,
                color = Color(0xFF424242),
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRequester) "Helper:" else "Requester:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = otherUserName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF424242)
                    )
                }

                if (theirRating != null) {
                    UserRatingDisplay(
                        averageRating = theirRating,
                        totalRatings = 1,
                        showCount = false
                    )
                }
            }

            HorizontalDivider()

            if (myRating == null) {
                Button(
                    onClick = onRateClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C63FF)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Rate $otherUserName")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "You rated: ",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    UserRatingDisplay(
                        averageRating = myRating,
                        totalRatings = 1,
                        showCount = false
                    )
                }
            }
        }
    }
}


@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.neighborhoodhelper.ui.feed

import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Badge

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.filled.VolunteerActivism
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.TaskCategory
import com.example.neighborhoodhelper.model.FriendRequest
import com.example.neighborhoodhelper.ui.profile.ProfileViewModel
import com.example.neighborhoodhelper.ui.friends.FriendsViewModel
import com.example.neighborhoodhelper.ui.chat.ChatViewModel
import com.example.neighborhoodhelper.utils.ImageUploadManager
import com.example.neighborhoodhelper.data.AppNotification


// Color Palette
val PrimaryPurple = Color(0xFF6C63FF)
val LightBackground = Color(0xFFF5F5F5)
val DarkGray = Color(0xFF424242)
val MediumGray = Color(0xFF757575)

@Composable
fun FeedScreen(navController: NavController) {
    val viewModel: FeedViewModel = viewModel() // Keep this at the top
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value


    var showCreatePost by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var navigateToSettings by remember { mutableStateOf(false) }
    var navigateToChatList by remember { mutableStateOf(false) }

    // Handle navigation
    LaunchedEffect(navigateToSettings) {
        if (navigateToSettings) {
            navController.navigate("settings")
            navigateToSettings = false
        }
    }

    LaunchedEffect(navigateToChatList) {
        if (navigateToChatList) {
            navController.navigate("chatList")
            navigateToChatList = false
        }
    }

    FeedContent(
        posts = posts,
        onWilling = { viewModel.accept(it) },
        onPostClick = { id -> navController.navigate("postDetail/$id") },
        onCreatePostClick = { showCreatePost = true },
        onMenuClick = { showMenu = true },
        onHomeClick = {
            viewModel.refreshFeed()
            refreshTrigger++
        },
        onMapClick = { navController.navigate("map") },
        onNotificationsClick = { showNotifications = true },
        onProfileClick = { showProfile = true },
        refreshTrigger = refreshTrigger,
        viewModel = viewModel,
        navController = navController
    )


    // Dialogs
    if (showCreatePost) {
        CreatePostDialog(onDismiss = { showCreatePost = false })
    }

    if (showMenu) {
        MenuDialog(onDismiss = { showMenu = false })
    }


    if (showNotifications) {

        LaunchedEffect(showNotifications) {
            if (showNotifications) {
                viewModel.markAllNotificationsAsViewed()
            }
        }

        NotificationsDialog(
            onDismiss = { showNotifications = false },
            navController = navController,
            viewModel = viewModel // Pass the same viewModel instance
        )

    }



    if (showProfile) {
        ProfileDialog(
            onDismiss = { showProfile = false },
            onNavigateToSettings = {
                showProfile = false
                navigateToSettings = true
            }
        )
    }
}

@Composable
fun FeedContent(
    posts: List<Post>,
    onWilling: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    refreshTrigger: Int,
    viewModel: FeedViewModel,
    navController: NavController
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TaskCategory>(TaskCategory.ALL) }

    Scaffold(
        containerColor = LightBackground,
        topBar = {
            CustomTopBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onMenuClick = onMenuClick
            )
        },
        bottomBar = {
            CustomBottomBar(
                navController = navController,
                onHomeClick = onHomeClick,
                onMapClick = onMapClick,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick,
                viewModel = viewModel
            )

        }

    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Category Filter Bar
            com.example.neighborhoodhelper.ui.components.CategoryFilterBar(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Create Post Button
                item {
                    CreatePostButton(onClick = onCreatePostClick)
                }

                // Filter posts by search query and category
                val filteredPosts = posts.filter {
                    val matchesSearch = searchQuery.isBlank() ||
                        it.username.contains(searchQuery, true) ||
                        it.content.contains(searchQuery, true)
                    val matchesCategory = selectedCategory == TaskCategory.ALL ||
                        it.category == selectedCategory.name
                    matchesSearch && matchesCategory
                }

                itemsIndexed(filteredPosts, key = { _, item -> item.id }) { _, post ->
                    PostCard(
                        post = post,
                        onWilling = { onWilling(post.id) },
                        onClick = { onPostClick(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Surface(
        color = PrimaryPurple,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Add safe spacing for system UI
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App Icon
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                // Search Bar
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        }
                    }
                )

                // Menu Icon
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePostButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Create Post",
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Create a post...",
                color = MediumGray,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PostCard(
    post: Post,
    onWilling: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category and Status badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val category = TaskCategory.fromString(post.category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(category.color.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.displayName,
                                tint = category.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = category.displayName,
                                color = category.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                com.example.neighborhoodhelper.ui.components.TaskStatusBadge(status = post.status)
            }

            // User Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.username.firstOrNull()?.toString() ?: "U",
                        fontWeight = FontWeight.SemiBold,
                        color = MediumGray,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color(0xFF1A1A1A)  // Explicit dark color
                    )
                    Text(
                        text = post.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575),  // Explicit gray color
                        fontSize = 12.sp
                    )
                }
            }

            // Post Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF424242),  // Explicit dark gray color
                fontSize = 14.sp
            )

            // Post Image
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "post image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE0E0E0))
                )
            }

            // Location if available
            if (!post.location.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MediumGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Stats row (likes, comments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${post.likes} Willing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray,
                    fontSize = 12.sp
                )
                Text(
                    text = "${post.comments} Comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray,
                    fontSize = 12.sp
                )
            }

            // Action Buttons
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val viewModel: FeedViewModel = viewModel()
                val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }
                val isWilling = post.willingUsers.contains(currentUserId)

                OutlinedButton(
                    onClick = onWilling,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isWilling) PrimaryPurple else Color.White,
                        contentColor = if (isWilling) Color.White else DarkGray
                    ),
                    border = BorderStroke(1.dp, if (isWilling) PrimaryPurple else Color(0xFFE0E0E0))
                ) {
                    Text(
                        text = if (isWilling) "Willing ✓" else "Willing",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Comment", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }


        }
    }
}

@Composable
fun CustomBottomBar(
    navController: NavController,
    onHomeClick: () -> Unit,
    onMapClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: FeedViewModel // ✅ Add this parameter
) {
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, "Home", Modifier.size(26.dp)) },
            selected = true,
            onClick = {
                onHomeClick()
                android.widget.Toast.makeText(
                    context,
                    "Refreshing feed...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, "Map", Modifier.size(26.dp)) },
            selected = false,
            onClick = onMapClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = {
                Box {
                    Icon(Icons.Outlined.Notifications, "Notifications", Modifier.size(26.dp))
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            },
            selected = false,
            onClick = { navController.navigate("notifications") },

            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountCircle, "Profile", Modifier.size(26.dp)) },
            selected = false,
            onClick = onProfileClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryPurple,
                unselectedIconColor = Color.Gray,
                indicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun CreatePostDialog(onDismiss: () -> Unit) {
    val viewModel: FeedViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var postText by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TaskCategory>(TaskCategory.OTHER) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imageUploadManager = remember { ImageUploadManager() }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Handle UI state
    LaunchedEffect(uiState) {
        when (uiState) {
            is FeedViewModel.UiState.Success -> {
                isPosting = false
                postText = ""
                location = ""
                selectedImageUri = null
                onDismiss()
                viewModel.resetUiState()
            }
            is FeedViewModel.UiState.Error -> {
                isPosting = false
            }
            is FeedViewModel.UiState.Loading -> {
                isPosting = true
            }
            else -> Unit
        }
    }

    Dialog(onDismissRequest = { if (!isPosting) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Create Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    maxLines = 6,
                    enabled = !isPosting,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple
                    )
                )

                // Image Preview
                selectedImageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Remove button
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Add Image Button
                if (selectedImageUri == null) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPosting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryPurple
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Add image",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add Photo")
                    }
                }

                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCategoryDropdown = !showCategoryDropdown },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPosting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = selectedCategory.color
                        ),
                        border = BorderStroke(1.dp, selectedCategory.color)
                    ) {
                        Icon(
                            imageVector = selectedCategory.icon,
                            contentDescription = selectedCategory.displayName,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedCategory.displayName,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showCategoryDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle dropdown"
                        )
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        TaskCategory.entries.filter { it != TaskCategory.ALL }.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = category.displayName,
                                            tint = category.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(category.displayName)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isPosting,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location"
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple
                    )
                )

                // Show error if any
                if (uiState is FeedViewModel.UiState.Error) {
                    Text(
                        text = (uiState as FeedViewModel.UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isPosting
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (postText.isNotBlank()) {
                                isPosting = true
                                viewModel.createPostWithImage(
                                    content = postText,
                                    imageUri = selectedImageUri,
                                    location = location.ifBlank { null },
                                    category = selectedCategory.name,
                                    context = context
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Please write something",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = postText.isNotBlank() && !isPosting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        )
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post")
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun MenuDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Surface(
                    color = PrimaryPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            "Menu",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    MenuItemCard(
                        icon = Icons.Default.Info,
                        text = "About",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Neighborhood Helper v1.0\nHelping neighbors connect and support each other",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )

                    MenuItemCard(
                        icon = Icons.Default.Help,
                        text = "Help & Support",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "For support, email: support@neighborhoodhelper.com",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    )

                    MenuItemCard(
                        icon = Icons.Default.Share,
                        text = "Share App",
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Check out Neighborhood Helper app! Connect with your neighbors and build a stronger community."
                                )
                                type = "text/plain"
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "Share via")
                            )
                        }
                    )

                    MenuItemCard(
                        icon = Icons.Default.Star,
                        text = "Rate Us",
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Thank you for your support! ⭐",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, text, tint = PrimaryPurple, modifier = Modifier.size(24.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}


@Composable
fun FriendItemWithChat(name: String, onMessageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MediumGray
                )
            }
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onMessageClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Message", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun FriendRequestItem(
    request: com.example.neighborhoodhelper.model.FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = request.fromUsername.firstOrNull()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MediumGray
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.fromUsername,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Sent you a friend request",
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Accept", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onReject,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Reject", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: com.example.neighborhoodhelper.model.User,
    onAddFriend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.firstOrNull()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MediumGray
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        fontSize = 12.sp,
                        color = MediumGray,
                        maxLines = 1
                    )
                }
            }

            Button(
                onClick = onAddFriend,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Add Friend",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Add", fontSize = 14.sp)
            }
        }
    }
}
@Composable
fun FriendItem(name: String, onMessageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MediumGray
                )
            }
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onMessageClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Message", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun NotificationsDialog(
    onDismiss: () -> Unit,
    navController: NavController,
    viewModel: FeedViewModel
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val context = LocalContext.current // Add this line

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Surface(
                    color = PrimaryPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            "Notifications",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "No notifications",
                                tint = MediumGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "No notifications yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MediumGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notification ->
                            NotificationItemCard(
                                notification = notification,
                                onClick = {
                                    notification.postId?.let { postId ->
                                        if (postId.isNotBlank()) {
                                            viewModel.navigateToPostFromNotification(
                                                postId,
                                                notification.id,
                                                navController
                                            )
                                            onDismiss()
                                        }
                                    } ?: run {
                                        viewModel.markNotificationAsRead(notification.id)
                                    }
                                },
                                onAccept = {
                                    notification.postId?.let { postId ->
                                        viewModel.acceptWillingUserRequest(
                                            notification.id,
                                            postId,
                                            notification.fromUserId
                                        ) { matchId ->
                                            // Navigate to matchTracking with matchId
                                            navController.navigate("matchTracking/$matchId")
                                            onDismiss()
                                        }
                                        Toast.makeText(
                                            context,
                                            "Match accepted! Opening map...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onReject = {
                                    viewModel.rejectWillingRequest(notification.id)
                                    Toast.makeText(
                                        context, // Changed from kotlin.context
                                        "Rejected willing request",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    val context = LocalContext.current // Add this line

    val icon = when (notification.type) {
        "like" -> Icons.Default.ThumbUp
        "comment" -> Icons.Default.Create
        "reply" -> Icons.Default.Reply
        "friend_request" -> Icons.Default.PersonAdd
        "message" -> Icons.Default.Message
        "WILLING" -> Icons.Default.VolunteerActivism
        "COMMENT" -> Icons.Default.Comment
        "REQUEST_ACCEPTED" -> Icons.Default.CheckCircle
        "REQUEST_REJECTED" -> Icons.Default.Cancel
        "LIKE" -> Icons.Default.ThumbUp
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead)
                Color(0xFFF8F8F8)
            else
                PrimaryPurple.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.message,
                        fontSize = 14.sp,
                        color = DarkGray,
                        fontWeight = if (notification.isRead)
                            FontWeight.Normal
                        else
                            FontWeight.SemiBold
                    )
                }

                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                }
            }

            if (notification.type == "WILLING" && notification.requiresAction) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onAccept()
                            Toast.makeText(
                                context,
                                "Accepted willing request from ${notification.fromUsername}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Accept", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onReject()
                            Toast.makeText(
                                context,
                                "Rejected willing request",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject", fontSize = 12.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: FeedViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()

    val currentUser by profileViewModel.currentUser.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var userBio by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }


    // Load current user data
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            userName = user.username
            userEmail = user.email
            userPhone = user.phoneNumber
            userBio = user.bio
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Surface(
                    color = PrimaryPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = Color.White
                            )
                        }
                        Text(
                            "Profile",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Profile Picture
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentUser?.avatarUrl?.isNotBlank() == true) {
                                AsyncImage(
                                    model = currentUser?.avatarUrl,
                                    contentDescription = "Profile picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = userName.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    item {
                        if (isEditing) {
                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple
                                )
                            )
                        } else {
                            Text(
                                text = userName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)  // Explicit dark color
                            )
                        }
                    }

                    item {
                        if (isEditing) {
                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = { userEmail = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple
                                )
                            )
                        } else {
                            Text(
                                text = userEmail.ifBlank { "No email set" },
                                fontSize = 14.sp,
                                color = Color(0xFF757575)  // Explicit gray color
                            )
                        }
                    }

                    item {
                        if (isEditing) {
                            OutlinedTextField(
                                value = userPhone,
                                onValueChange = { userPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple
                                )
                            )
                        } else if (userPhone.isNotBlank()) {
                            Text(
                                text = userPhone,
                                fontSize = 14.sp,
                                color = Color(0xFF757575)  // Explicit gray color
                            )
                        }
                    }

                    item {
                        if (isEditing) {
                            OutlinedTextField(
                                value = userBio,
                                onValueChange = { userBio = it },
                                label = { Text("Bio") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple
                                )
                            )
                        } else if (userBio.isNotBlank()) {
                            Text(
                                text = userBio,
                                fontSize = 14.sp,
                                color = Color(0xFF424242),  // Explicit dark gray color
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }

                    item {
                        if (isEditing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isEditing = false
                                        // Reset values
                                        currentUser?.let { user ->
                                            userName = user.username
                                            userEmail = user.email
                                            userPhone = user.phoneNumber
                                            userBio = user.bio
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        profileViewModel.updateProfile(
                                            username = userName,
                                            email = userEmail,
                                            phoneNumber = userPhone,
                                            bio = userBio
                                        )
                                        isEditing = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryPurple
                                    )
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ProfileMenuItem(
                                    icon = Icons.Default.Person,
                                    text = "Edit Profile",
                                    onClick = { isEditing = true }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ProfileMenuItem(
                                    icon = Icons.Default.Settings,
                                    text = "Settings",
                                    onClick = {
                                        onNavigateToSettings()
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


                                ProfileMenuItem(
                                    icon = Icons.Default.Lock,
                                    text = "Privacy",
                                    onClick = {
                                        android.widget.Toast.makeText(
                                            context,  // Use context here instead of LocalContext.current
                                            "Privacy settings coming soon",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )


                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ProfileMenuItem(
                                    icon = Icons.Default.Info,
                                    text = "About",
                                    onClick = {
                                        android.widget.Toast.makeText(
                                            context,  // Use context here instead of LocalContext.current
                                            "Neighborhood Helper v1.0\nHelping neighbors connect and support each other",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                ProfileMenuItem(
                                    icon = Icons.Default.Logout,
                                    text = "Logout",
                                    onClick = {
                                        showLogoutDialog = true
                                    }
                                )

                                // Logout Confirmation Dialog
                                if (showLogoutDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showLogoutDialog = false },
                                        title = { Text("Logout") },
                                        text = { Text("Are you sure you want to logout?") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showLogoutDialog = false
                                                    profileViewModel.logout()
                                                    onDismiss()
                                                }
                                            ) {
                                                Text("Yes", color = Color.Red)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showLogoutDialog = false }) {
                                                Text("Cancel")
                                            }
                                        }
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

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = text, tint = PrimaryPurple)
        Text(text = text, fontSize = 16.sp)
    }
}

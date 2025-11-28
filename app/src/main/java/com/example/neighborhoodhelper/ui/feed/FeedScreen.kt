@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.neighborhoodhelper.ui.feed

import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Badge

import com.example.neighborhoodhelper.model.FriendRequest

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
import com.example.neighborhoodhelper.model.Notification
import com.example.neighborhoodhelper.model.NotificationType
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.ui.profile.ProfileViewModel
import com.example.neighborhoodhelper.ui.friends.FriendsViewModel
import com.example.neighborhoodhelper.ui.chat.ChatViewModel
import com.example.neighborhoodhelper.utils.ImageUploadManager

// Color Palette
val PrimaryPurple = Color(0xFF6C63FF)
val LightBackground = Color(0xFFF5F5F5)
val DarkGray = Color(0xFF424242)
val MediumGray = Color(0xFF757575)

@Composable
fun FeedScreen(navController: NavController) {
    val viewModel: FeedViewModel = viewModel()
    val postsState = viewModel.posts.collectAsStateWithLifecycle()
    val posts = postsState.value

    var showCreatePost by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showFriends by remember { mutableStateOf(false) }
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
        onFriendsClick = { showFriends = true },
        onNotificationsClick = { showNotifications = true },
        onProfileClick = { showProfile = true },
        refreshTrigger = refreshTrigger
    )

    // Dialogs
    if (showCreatePost) {
        CreatePostDialog(onDismiss = { showCreatePost = false })
    }

    if (showMenu) {
        MenuDialog(onDismiss = { showMenu = false })
    }

    if (showFriends) {
        FriendsDialog(
            onDismiss = { showFriends = false },
            onNavigateToChat = {
                showFriends = false
                navigateToChatList = true
            }
        )
    }

    if (showNotifications) {
        NotificationsDialog(onDismiss = { showNotifications = false })
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
    onWilling: (postId: String) -> Unit,
    onPostClick: (postId: String) -> Unit,
    onCreatePostClick: () -> Unit,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    refreshTrigger: Int
) {
    var searchQuery by remember { mutableStateOf("") }

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
                onHomeClick = onHomeClick,
                onFriendsClick = onFriendsClick,
                onNotificationsClick = onNotificationsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Create Post Button
            item {
                CreatePostButton(onClick = onCreatePostClick)
            }

            // Filter posts by search query
            val filteredPosts = if (searchQuery.isBlank()) posts else posts.filter {
                it.username.contains(searchQuery, true) || it.content.contains(searchQuery, true)
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
                Column {
                    Text(
                        text = post.username,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = post.timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Post Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyMedium,
                color = DarkGray,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onWilling,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = DarkGray
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Text("Willing", fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
    onHomeClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val viewModel: FeedViewModel = viewModel()
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
            icon = { Icon(Icons.Outlined.Person, "Friends", Modifier.size(26.dp)) },
            selected = false,
            onClick = onFriendsClick,
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
            onClick = onNotificationsClick,
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isPosting by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
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
                isUploadingImage = false
                postText = ""
                location = ""
                selectedImageUri = null
                onDismiss()
                viewModel.resetUiState()
            }
            is FeedViewModel.UiState.Error -> {
                isPosting = false
                isUploadingImage = false
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
                            viewModel.createPostWithImage(
                                content = postText,
                                imageUri = selectedImageUri,
                                location = location.ifBlank { null },
                                context = context
                            )
                        },
                        enabled = !isPosting && postText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        if (isPosting || isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            if (isUploadingImage) "Uploading..."
                            else if (isPosting) "Posting..."
                            else "Post"
                        )
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
fun FriendsDialog(
    onDismiss: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val friendsViewModel: FriendsViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()

    val friends by friendsViewModel.friends.collectAsStateWithLifecycle()
    val friendRequests by friendsViewModel.friendRequests.collectAsStateWithLifecycle()
    val searchResults by friendsViewModel.searchResults.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                // Header
                Surface(
                    color = PrimaryPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
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
                                "Friends",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Tabs
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = PrimaryPurple,
                            contentColor = Color.White
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("My Friends") }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Requests")
                                        if (friendRequests.isNotEmpty()) {
                                            Badge { Text("${friendRequests.size}") }
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Add Friends") }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // My Friends Tab
                        if (friends.isEmpty()) {
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
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "No friends",
                                        tint = MediumGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "No friends yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MediumGray
                                    )
                                    Text(
                                        "Add friends to start connecting!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MediumGray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(friends) { friend ->
                                    FriendItemWithChat(
                                        name = friend.username,
                                        onMessageClick = {
                                            chatViewModel.openChatWithUser(friend.id)
                                            onNavigateToChat()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Friend Requests Tab
                        if (friendRequests.isEmpty()) {
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
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "No requests",
                                        tint = MediumGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "No friend requests",
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(friendRequests) { request ->
                                    FriendRequestItem(
                                        request = request,
                                        onAccept = {
                                            friendsViewModel.acceptFriendRequest(request.id, request.fromUserId)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Friend request accepted",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onReject = {
                                            friendsViewModel.rejectFriendRequest(request.id, request.fromUserId)
                                            android.widget.Toast.makeText(
                                                context,
                                                "Friend request rejected",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Add Friends Tab
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    friendsViewModel.searchUsers(it)
                                },
                                placeholder = { Text("Search users...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Search, "Search")
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple
                                )
                            )

                            if (searchQuery.isBlank()) {
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
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = MediumGray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "Search for users to add as friends",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MediumGray
                                        )
                                    }
                                }
                            } else if (searchResults.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No users found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MediumGray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(searchResults) { user ->
                                        UserSearchItem(
                                            user = user,
                                            onAddFriend = {
                                                friendsViewModel.sendFriendRequest(user.id)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Friend request sent to ${user.username}",
                                                    android.widget.Toast.LENGTH_SHORT
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
fun NotificationsDialog(onDismiss: () -> Unit) {
    val viewModel: FeedViewModel = viewModel()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

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
                                    viewModel.markNotificationAsRead(notification.id)
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
    notification: Notification,
    onClick: () -> Unit
) {
    val icon = when (notification.type) {
        "LIKE" -> Icons.Default.ThumbUp
        "COMMENT" -> Icons.Default.Create
        "REPLY" -> Icons.Default.Reply
        "FRIEND_REQUEST" -> Icons.Default.PersonAdd
        "MESSAGE" -> Icons.Default.Message
        else -> Icons.Default.Notifications
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color(0xFFF8F8F8) else PrimaryPurple.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.SemiBold
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple)
                )
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
                                fontWeight = FontWeight.Bold
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
                                color = MediumGray
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
                                color = MediumGray
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
                                color = DarkGray,
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
                                        viewModel.showLogoutDialog = true
                                    }
                                )
                                val showLogoutDialog = false
                                if (showLogoutDialog) {
                                    AlertDialog(
                                        onDismissRequest = { var showLogoutDialog = false },
                                        title = { Text("Logout") },
                                        text = { Text("Are you sure you want to logout?") },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    var showLogoutDialog = false
                                                    // Logout logic here
                                                }
                                            ) {
                                                Text("Yes", color = Color.Red)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { var showLogoutDialog = false }) {
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

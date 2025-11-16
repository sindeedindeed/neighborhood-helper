@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.neighborhoodhelper.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.neighborhoodhelper.model.Post
import com.example.neighborhoodhelper.model.Comment

// Color Palette
val PrimaryPurple = Color(0xFF6B3FA0)
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

    FeedContent(
        posts = posts,
        onWilling = { viewModel.accept(it) },
        onPostClick = { id -> navController.navigate("postDetail/$id") },
        onCreatePostClick = { showCreatePost = true },
        onMenuClick = { showMenu = true },
        onHomeClick = { refreshTrigger++ },
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
        FriendsDialog(onDismiss = { showFriends = false })
    }
    if (showNotifications) {
        NotificationsDialog(onDismiss = { showNotifications = false })
    }
    if (showProfile) {
        ProfileDialog(onDismiss = { showProfile = false })
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
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Outlined.Home, "Home", Modifier.size(26.dp)) },
            selected = true,
            onClick = onHomeClick,
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
            icon = { Icon(Icons.Outlined.Notifications, "Notifications", Modifier.size(26.dp)) },
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
    var postText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Post")
                    }
                }
            }
        }
    }
}

@Composable
fun MenuDialog(onDismiss: () -> Unit) {
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
                    MenuItemCard(Icons.Default.Info, "About")
                    MenuItemCard(Icons.Default.Help, "Help & Support")
                    MenuItemCard(Icons.Default.Share, "Share App")
                    MenuItemCard(Icons.Default.Star, "Rate Us")
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { },
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
fun FriendsDialog(onDismiss: () -> Unit) {
    val friends = remember {
        listOf(
            "Maishan Nadis", "Safwat Bushra", "Faiza Tashmeah",
            "Mahdi Hasan", "Sarah Ahmed", "Rahim Khan"
        )
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
                            "Friends",
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(friends) { friend ->
                        FriendItem(name = friend)
                    }
                }
            }
        }
    }
}

@Composable
fun FriendItem(name: String) {
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
                onClick = { },
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
    val notifications = remember {
        listOf(
            Triple("John liked your post", "2m ago", Icons.Default.ThumbUp),
            Triple("Sarah commented on your post", "5m ago", Icons.Default.Create),
            Triple("You have a new follower", "1h ago", Icons.Default.PersonAdd),
            Triple("Mike shared your post", "3h ago", Icons.Default.Share),
            Triple("Emma mentioned you", "5h ago", Icons.Default.Notifications)
        )
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
                            "Notifications",
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { (notification, time, icon) ->
                        NotificationItem(notification, time, icon)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: String,
    time: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
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
                Text(text = notification, fontSize = 14.sp, color = DarkGray)
                Text(text = time, fontSize = 12.sp, color = MediumGray)
            }
        }
    }
}

@Composable
fun ProfileDialog(onDismiss: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Mahdi Hasan") }
    var userEmail by remember { mutableStateOf("mahdi@email.com") }

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
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.firstOrNull()?.toString() ?: "M",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
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
                                text = userEmail,
                                fontSize = 14.sp,
                                color = MediumGray
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
                                    onClick = { isEditing = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { isEditing = false },
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
                                ProfileMenuItem(Icons.Default.Settings, "Settings", onClick = {})
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileMenuItem(Icons.Default.Notifications, "Notification Settings", onClick = {})
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileMenuItem(Icons.Default.Lock, "Privacy", onClick = {})
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileMenuItem(Icons.Default.Info, "About", onClick = {})
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ProfileMenuItem(Icons.AutoMirrored.Filled.ExitToApp, "Logout", onClick = {})
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
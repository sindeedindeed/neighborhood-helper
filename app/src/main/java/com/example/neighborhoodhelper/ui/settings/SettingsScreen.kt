package com.example.neighborhoodhelper.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neighborhoodhelper.model.UserSettings
import com.example.neighborhoodhelper.ui.auth.LandingActivity
import com.example.neighborhoodhelper.ui.feed.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notificationsEnabled by remember { mutableStateOf(true) }
    var friendRequestNotifications by remember { mutableStateOf(true) }
    var messageNotifications by remember { mutableStateOf(true) }
    var postNotifications by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        settings?.let {
            notificationsEnabled = it.notificationsEnabled
            friendRequestNotifications = it.friendRequestNotifications
            messageNotifications = it.messageNotifications
            postNotifications = it.postNotifications
            soundEnabled = it.soundEnabled
            vibrationEnabled = it.vibrationEnabled
        }
    }

    // Handle logout state
    LaunchedEffect(uiState) {
        if (uiState is SettingsViewModel.UiState.LoggedOut) {
            val intent = Intent(context, LandingActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Settings
            Text(
                "General",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsSwitchItem(
                        title = "Enable Notifications",
                        checked = notificationsEnabled,
                        onCheckedChange = {
                            notificationsEnabled = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(notificationsEnabled = it))
                            }
                        }
                    )
                }
            }

            // Notification Settings
            Text(
                "Notification Types",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsSwitchItem(
                        title = "Friend Requests",
                        description = "Get notified when someone sends you a friend request",
                        checked = friendRequestNotifications,
                        onCheckedChange = {
                            friendRequestNotifications = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(friendRequestNotifications = it))
                            }
                        },
                        enabled = notificationsEnabled
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchItem(
                        title = "Messages",
                        description = "Get notified about new messages",
                        checked = messageNotifications,
                        onCheckedChange = {
                            messageNotifications = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(messageNotifications = it))
                            }
                        },
                        enabled = notificationsEnabled
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchItem(
                        title = "Posts & Comments",
                        description = "Get notified about likes and comments",
                        checked = postNotifications,
                        onCheckedChange = {
                            postNotifications = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(postNotifications = it))
                            }
                        },
                        enabled = notificationsEnabled
                    )
                }
            }

            // Sound & Vibration
            Text(
                "Sound & Vibration",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsSwitchItem(
                        title = "Sound",
                        description = "Play notification sound",
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(soundEnabled = it))
                            }
                        },
                        enabled = notificationsEnabled
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    SettingsSwitchItem(
                        title = "Vibration",
                        description = "Vibrate on notifications",
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            settings?.let { s ->
                                viewModel.updateSettings(s.copy(vibrationEnabled = it))
                            }
                        },
                        enabled = notificationsEnabled
                    )
                }
            }

            // Account Section
            Text(
                "Account",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Logout Button
                    TextButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Logout",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Red
                                )
                                Text(
                                    "Sign out of your account",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            if (uiState is SettingsViewModel.UiState.Success) {
                Text(
                    "Settings saved successfully",
                    color = Color.Green,
                    fontSize = 14.sp
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    viewModel.resetUiState()
                }
            }

            if (uiState is SettingsViewModel.UiState.Error) {
                Text(
                    (uiState as SettingsViewModel.UiState.Error).message,
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (description != null) {
                Text(
                    description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryPurple,
                checkedTrackColor = PrimaryPurple.copy(alpha = 0.5f)
            )
        )
    }
}
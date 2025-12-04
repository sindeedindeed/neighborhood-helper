package com.example.neighborhoodhelper.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.ActiveMatch
import com.example.neighborhoodhelper.ui.theme.DeepPrimary
import com.example.neighborhoodhelper.ui.theme.DeepPrimaryDark
import com.example.neighborhoodhelper.ui.theme.MediumGray
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import java.util.Locale

@Composable
fun MapListScreen(navController: NavController, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val repository = FirebaseRepository()

    val userId = auth.currentUser?.uid ?: return
    val activeMatches by repository.observeActiveMatchesForUser(userId).collectAsStateWithLifecycle(emptyList())

    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        // Header
        Column {
            Surface(
                color = DeepPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Text(
                        "Active Matches",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Content
            if (activeMatches.isEmpty()) {
                // No active matches
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
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "No matches",
                            tint = MediumGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No Active Matches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediumGray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Accept a willing request to start tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray
                        )
                    }
                }
            } else {
                // List of active matches
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeMatches) { match ->
                        ActiveMatchCard(
                            match = match,
                            currentUserId = userId,
                            onClick = {
                                // Navigate to MatchTrackingScreen with matchId
                                navController.navigate("matchTracking/${match.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveMatchCard(match: ActiveMatch, currentUserId: String, onClick: () -> Unit) {
    val isCurrentUserHelper = match.helperId == currentUserId
    val contactLabel = if (isCurrentUserHelper) "Requester" else "Helper"
    val contactName = if (isCurrentUserHelper) match.requesterName else match.helperName
    val contactPhone = if (isCurrentUserHelper) match.requesterPhone else match.helperPhone
    val contactRating = if (isCurrentUserHelper) match.requesterRating else match.helperRating
    val contactRatingCount = if (isCurrentUserHelper) match.requesterRatingCount else match.helperRatingCount
    val ratingDisplay = if (contactRatingCount > 0) {
        "${String.format(Locale.getDefault(), "%.1f", contactRating)} (${contactRatingCount})"
    } else {
        "No ratings yet"
    }
    val phoneDisplay = contactPhone.ifBlank { "Phone not shared" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.requesterName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPrimaryDark
                    )
                    Text(
                        text = "Requester",
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (match.isProximityReached) Color(0xFF4CAF50) else Color(0xFFFFA726)
                ) {
                    Text(
                        text = if (match.isProximityReached) "Arrived" else "En route",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Divider
            HorizontalDivider(modifier = Modifier.fillMaxWidth())

            // Helper info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DeepPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.helperName.firstOrNull()?.toString() ?: "H",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = match.helperName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepPrimaryDark
                    )
                    Text(
                        text = "Helper",
                        fontSize = 12.sp,
                        color = MediumGray
                    )
                }

                // Distance
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f m", match.distance),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPrimary
                    )
                    Text(
                        text = "away",
                        fontSize = 11.sp,
                        color = MediumGray
                    )
                }
            }

            // View map button
            Button(
                onClick = { /* Will be handled by parent onClick */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "View map",
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View on Map", fontSize = 14.sp)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$contactLabel details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MediumGray,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = contactName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeepPrimaryDark
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = ratingDisplay,
                        fontSize = 13.sp,
                        color = DeepPrimaryDark
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = DeepPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = phoneDisplay,
                        fontSize = 13.sp,
                        color = DeepPrimaryDark
                    )
                }
            }
        }
    }
}

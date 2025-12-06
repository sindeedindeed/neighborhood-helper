package com.example.neighborhoodhelper.ui.match

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neighborhoodhelper.ui.components.RatingDialog
import com.example.neighborhoodhelper.ui.theme.DeepPrimary
import com.example.neighborhoodhelper.ui.theme.DeepPrimaryDark
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun MatchTrackingScreen(
    matchId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MatchTrackingViewModel = viewModel()
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val routePolyline by viewModel.routePolyline.collectAsStateWithLifecycle()
    val isHelper by viewModel.isHelper.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var showProximityDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmation by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()

    // Initialize match tracking
    LaunchedEffect(matchId) {
        viewModel.initialize(matchId)
    }

    // Start location updates when permission is granted
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        android.util.Log.d("MatchTracking", "📍 Location update: ${location.latitude}, ${location.longitude}")
                        viewModel.updateMyLocation(location.latitude, location.longitude)
                    }
                }
        }
    }

    // Check proximity
    LaunchedEffect(activeMatch?.distance) {
        if (activeMatch?.distance != null && activeMatch!!.distance <= 20f && !activeMatch!!.isProximityReached) {
            viewModel.markProximityReached()
            showProximityDialog = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Proximity reached dialog
    if (showProximityDialog && activeMatch != null) {
        AlertDialog(
            onDismissRequest = { showProximityDialog = false },
            title = { Text("You've Arrived! 🎉") },
            text = {
                Column {
                    Text("You are now within 20 meters of each other.")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isHelper) {
                        Text(
                            text = "Requester's Phone: ${activeMatch?.requesterPhone}",
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Helper's Phone: ${activeMatch?.helperPhone}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showProximityDialog = false }) {
                    Text("Got it!")
                }
            }
        )
    }

    // Complete confirmation dialog
    if (showCompleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmation = false },
            title = { Text("Mark Task Complete?") },
            text = { Text("Have you completed this task? You'll be asked to rate your experience.") },
            confirmButton = {
                Button(onClick = {
                    showCompleteConfirmation = false
                    viewModel.completeMatch()
                    showRatingDialog = true
                }) {
                    Text("Yes, Complete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmation = false }) {
                    Text("Not Yet")
                }
            }
        )
    }

    // Rating dialog
    if (showRatingDialog && activeMatch != null) {
        val otherUserName = if (isHelper) activeMatch!!.requesterName else activeMatch!!.helperName
        RatingDialog(
            userName = otherUserName,
            onDismiss = {
                showRatingDialog = false
                onComplete()
            },
            onSubmit = { rating, review ->
                viewModel.submitRating(rating, review)
                showRatingDialog = false
                onComplete()
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        if (activeMatch != null) {
            val requesterPos = LatLng(activeMatch!!.requesterLat, activeMatch!!.requesterLon)
            val helperPos = LatLng(activeMatch!!.helperLat, activeMatch!!.helperLon)
            val myPos = if (isHelper) helperPos else requesterPos

            // Log marker positions for debugging
            LaunchedEffect(requesterPos, helperPos) {
                android.util.Log.d("MatchTracking", "📍 Marker positions - Requester: $requesterPos, Helper: $helperPos")
            }

            LaunchedEffect(myPos) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(myPos, 15f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Requester marker
                Marker(
                    state = rememberUpdatedMarkerState(position = requesterPos),
                    title = activeMatch!!.requesterName,
                    snippet = "Task Location"
                )

                // Helper marker
                Marker(
                    state = rememberUpdatedMarkerState(position = helperPos),
                    title = activeMatch!!.helperName,
                    snippet = "Helper"
                )

                // Route polyline
                if (routePolyline.isNotEmpty()) {
                    Polyline(
                        points = routePolyline,
                        color = Color(0xFF2196F3), // Bright blue for better visibility
                        width = 15f
                    )
                }
            }
        }

        // Back button
        Card(
            modifier = Modifier
                .padding(16.dp)
                .zIndex(1f),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DeepPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Combined the "en route" tooltip with the review and number information
        if (activeMatch != null) {
            val partnerName = if (isHelper) activeMatch!!.requesterName else activeMatch!!.helperName
            val partnerPhone = if (isHelper) activeMatch!!.requesterPhone else activeMatch!!.helperPhone
            val partnerRating = if (isHelper) activeMatch!!.requesterRating else activeMatch!!.helperRating
            val partnerRatingCount = if (isHelper) activeMatch!!.requesterRatingCount else activeMatch!!.helperRatingCount
            val ratingLabel = if (partnerRatingCount > 0) {
                "${String.format(Locale.getDefault(), "%.1f", partnerRating)} • ${partnerRatingCount} ratings"
            } else {
                "No ratings yet"
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 16.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = partnerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (activeMatch!!.isProximityReached) "Arrived!" else "En route...",
                        fontSize = 14.sp,
                        color = if (activeMatch!!.isProximityReached) Color(0xFF4CAF50) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                            text = ratingLabel,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    // Call button
                    if (partnerPhone.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$partnerPhone"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Call",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call $partnerPhone", fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Distance card at bottom
        if (activeMatch != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Distance",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f m", activeMatch!!.distance),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Distance",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        if (activeMatch!!.isProximityReached) {
                            Button(
                                onClick = { showCompleteConfirmation = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Complete",
                                    tint = DeepPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark Complete", color = DeepPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Permission request overlay
        if (!hasLocationPermission) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Location",
                        tint = DeepPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Location Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPrimaryDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable location to track your position in real-time",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPrimary)
                    ) {
                        Text("Enable Location", color = Color.White)
                    }
                }
            }
        }
    }
}

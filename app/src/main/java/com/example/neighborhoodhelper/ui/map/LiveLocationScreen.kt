package com.example.neighborhoodhelper.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import java.util.Locale
import com.example.neighborhoodhelper.ui.theme.DeepPrimary
import com.example.neighborhoodhelper.ui.theme.DeepPrimaryDark
import com.example.neighborhoodhelper.ui.theme.MediumGray

@SuppressLint("MissingPermission")
@Composable
fun LiveLocationScreen(
    context: Context,
    lat: Double,
    lon: Double,
    markerTitle: String,
    onBack: (() -> Unit)? = null
) {
    // State for calculated distance and permission
    var distance by remember { mutableStateOf<Float?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        )
    }

    // Fused Location Provider
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Target location
    val targetLocation = remember { LatLng(lat, lon) }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(targetLocation, 13f)
    }

    // Function to calculate distance
    fun calculateDistance(userLat: Double, userLon: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, lat, lon, results)
        distance = results[0] / 1000f // Convert to km
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Permission granted, get location
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val newUserLocation = LatLng(it.latitude, it.longitude)
                            userLocation = newUserLocation
                            calculateDistance(it.latitude, it.longitude)

                            // Adjust camera to show both markers
                            val bounds = LatLngBounds.Builder()
                                .include(targetLocation)
                                .include(newUserLocation)
                                .build()

                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngBounds(bounds, 200)
                            )
                        }
                    }
            } catch (_: SecurityException) {
                // Handle permission error
            }
        }
    }

    // Handle location updates when permission changes
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val newUserLocation = LatLng(it.latitude, it.longitude)
                            userLocation = newUserLocation
                            calculateDistance(it.latitude, it.longitude)

                            // Adjust camera to show both markers
                            val bounds = LatLngBounds.Builder()
                                .include(targetLocation)
                                .include(newUserLocation)
                                .build()

                            cameraPositionState.move(
                                CameraUpdateFactory.newLatLngBounds(bounds, 200)
                            )
                        }
                    }
            } catch (_: SecurityException) {
                // Permission not granted
            }
        } else {
            // Request permission
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map
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
            // Requester marker (blue)
            Marker(
                state = MarkerState(position = targetLocation),
                title = markerTitle,
                snippet = "Requester Location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )

            // User location marker (red)
            userLocation?.let { userPos ->
                Marker(
                    state = MarkerState(position = userPos),
                    title = "Your Location",
                    snippet = "Current Position",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }

        // Top Bar with Back Button
        if (onBack != null) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DeepPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Stylish Location Indicator Card
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (onBack != null) 80.dp else 16.dp)
                .padding(horizontal = 16.dp)
                .zIndex(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DeepPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = markerTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPrimaryDark
                    )
                    Text(
                        text = "Current Location",
                        fontSize = 14.sp,
                        color = MediumGray
                    )
                }
            }
        }

        // Distance Counter Card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .zIndex(1f),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = DeepPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Distance",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = when {
                            !hasLocationPermission -> "Permission Required"
                            distance != null -> String.format(Locale.getDefault(), "%.1f km", distance)
                            else -> "Calculating..."
                        },
                        fontSize = if (!hasLocationPermission) 16.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (!hasLocationPermission) "Tap to enable location" else "Distance to location",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Permission request card overlay (only shows when permission is needed)
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
                        text = "Enable location access to calculate distance",
                        fontSize = 14.sp,
                        color = MediumGray
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

        // Live Tracking Indicator
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(1f)
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasLocationPermission) DeepPrimary else MediumGray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasLocationPermission) "Live" else "Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasLocationPermission) DeepPrimary else MediumGray
                    )
                }
            }
        }
    }
}

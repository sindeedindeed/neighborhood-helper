package com.example.neighborhoodhelper.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// helper: create a blue/white pin marker as a BitmapDrawable
fun createBlueWhiteMarkerDrawable(context: Context, sizeDp: Int = 56, blueColor: Int = 0xFF2196F3.toInt()): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (sizeDp * density).toInt().coerceAtLeast(32)
    val bmp = Bitmap.createBitmap(sizePx, (sizePx * 1.3f).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val cx = sizePx / 2f
    val cy = sizePx / 2f
    val radius = sizePx / 2.5f
    val pinHeight = sizePx * 0.4f

    // Draw pin body (teardrop shape)
    paint.color = blueColor
    paint.style = Paint.Style.FILL

    // Main circle of the pin
    canvas.drawCircle(cx, cy, radius, paint)

    // Pin point (triangle pointing down)
    val path = android.graphics.Path()
    path.moveTo(cx - radius * 0.3f, cy + radius * 0.7f)
    path.lineTo(cx + radius * 0.3f, cy + radius * 0.7f)
    path.lineTo(cx, cy + radius + pinHeight)
    path.close()
    canvas.drawPath(path, paint)

    // Inner white circle
    paint.color = AndroidColor.WHITE
    canvas.drawCircle(cx, cy, radius * 0.65f, paint)

    // Draw center location icon
    val icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)?.mutate()
    icon?.setTint(blueColor)
    val iconSize = (radius * 0.8f).toInt()
    val left = (cx - iconSize / 2f).toInt()
    val top = (cy - iconSize / 2f).toInt()
    icon?.setBounds(left, top, left + iconSize, top + iconSize)
    icon?.draw(canvas)

    return BitmapDrawable(context.resources, bmp)
}

@SuppressLint("MissingPermission")
@Composable
fun LiveLocationScreen(
    context: Context,
    lat: Double,
    lon: Double,
    markerTitle: String,
    onBack: (() -> Unit)? = null
) {
    // Setup osmdroid configuration
    Configuration.getInstance().load(
        context,
        context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    )

    // State for calculated distance and permission
    var distance by remember { mutableStateOf<Float?>(null) }
    var userLocation by remember { mutableStateOf<Location?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // LocationManager for getting user's location
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    // Function to calculate distance
    fun calculateDistance(userLat: Double, userLon: Double) {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, lat, lon, results)
        distance = results[0] / 1000f // Convert to km
    }

    // LocationListener for real-time updates
    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                userLocation = location
                calculateDistance(location.latitude, location.longitude)
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Permission granted, start location updates
            try {
                // Get last known location first
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastKnownLocation?.let {
                    userLocation = it
                    calculateDistance(it.latitude, it.longitude)
                }

                // Request location updates
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    10f,   // 10 meters
                    locationListener
                )
            } catch (e: SecurityException) {
                // Handle permission error
            }
        }
    }

    // Handle location updates when permission changes
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                // Get last known location
                val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                lastKnownLocation?.let {
                    userLocation = it
                    calculateDistance(it.latitude, it.longitude)
                }

                // Start location updates
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    10f,   // 10 meters
                    locationListener
                )
            } catch (e: SecurityException) {
                // Permission not granted
            }
        } else {
            // Request permission
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Cleanup location updates when composable is disposed
    DisposableEffect(hasLocationPermission) {
        onDispose {
            if (hasLocationPermission) {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (e: SecurityException) {
                    // Ignore
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map View
        AndroidView(
            factory = {
                MapView(it).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0) // Zoomed out a bit to show both markers
                    controller.setCenter(GeoPoint(lat, lon))

                    // Requester marker (styled blue/white)
                    val requesterMarker = Marker(this)
                    requesterMarker.position = GeoPoint(lat, lon)
                    requesterMarker.title = markerTitle
                    requesterMarker.snippet = "Requester Location"
                    // apply standardized stylish drawable and anchor to bottom-center
                    try {
                        requesterMarker.icon = createBlueWhiteMarkerDrawable(this.context, sizeDp = 56)
                    } catch (e: Exception) {
                        // fallback to defaults if something goes wrong
                    }
                    requesterMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    overlays.add(requesterMarker)

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { mapView ->
                // Update user location marker when location changes
                userLocation?.let { location ->
                    // Remove existing user marker if any
                    mapView.overlays.removeAll { overlay ->
                        overlay is Marker && overlay.title == "Your Location"
                    }

                    // Add user location marker (styled same as requester)
                    val userMarker = Marker(mapView)
                    userMarker.position = GeoPoint(location.latitude, location.longitude)
                    userMarker.title = "Your Location"
                    userMarker.snippet = "Current Position"

                    // Use the same blue/white styled drawable (slightly smaller)
                    try {
                        userMarker.icon = createBlueWhiteMarkerDrawable(mapView.context, sizeDp = 48)
                    } catch (e: Exception) {
                        // Fallback to default marker
                    }
                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

                    mapView.overlays.add(userMarker)
                    mapView.invalidate()
                }
            }
        )

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
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF2196F3),
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
                        .background(Color(0xFF2196F3)),
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
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = "Current Location",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)),
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
                            distance != null -> String.format("%.1f km", distance)
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
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Location Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enable location access to calculate distance",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
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
                            .background(if (hasLocationPermission) Color(0xFF2196F3) else Color(0xFF999999))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasLocationPermission) "Live" else "Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasLocationPermission) Color(0xFF2196F3) else Color(0xFF999999)
                    )
                }
            }
        }
    }
}

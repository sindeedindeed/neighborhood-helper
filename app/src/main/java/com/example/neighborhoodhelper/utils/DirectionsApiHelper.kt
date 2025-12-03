package com.example.neighborhoodhelper.utils

import android.util.Log
import com.example.neighborhoodhelper.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

object DirectionsApiHelper {
    private const val TAG = "DirectionsApiHelper"

    // API key is securely loaded from local.properties (gitignored)
    // To set your key: Add MAPS_API_KEY=your_key_here to local.properties file
    private val API_KEY = BuildConfig.MAPS_API_KEY

    data class RouteResult(
        val polyline: List<LatLng>,
        val distanceMeters: Int,
        val durationSeconds: Int,
        val distanceText: String,
        val durationText: String
    )

    suspend fun getDirections(
        origin: LatLng,
        destination: LatLng
    ): Result<RouteResult> = withContext(Dispatchers.IO) {
        try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destStr = "${destination.latitude},${destination.longitude}"

            val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${URLEncoder.encode(originStr, "UTF-8")}" +
                    "&destination=${URLEncoder.encode(destStr, "UTF-8")}" +
                    "&mode=driving" +
                    "&key=$API_KEY"

            val response = URL(urlString).readText()
            val json = JSONObject(response)

            if (json.getString("status") != "OK") {
                Log.e(TAG, "Directions API error: ${json.getString("status")}")
                return@withContext Result.failure(Exception("Unable to get directions"))
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) {
                return@withContext Result.failure(Exception("No routes found"))
            }

            val route = routes.getJSONObject(0)
            val legs = route.getJSONArray("legs").getJSONObject(0)

            val distance = legs.getJSONObject("distance")
            val duration = legs.getJSONObject("duration")

            val polylineEncoded = route.getJSONObject("overview_polyline").getString("points")
            val polylinePoints = decodePolyline(polylineEncoded)

            Result.success(
                RouteResult(
                    polyline = polylinePoints,
                    distanceMeters = distance.getInt("value"),
                    durationSeconds = duration.getInt("value"),
                    distanceText = distance.getString("text"),
                    durationText = duration.getString("text")
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting directions", e)
            Result.failure(e)
        }
    }

    // Decode Google's encoded polyline format
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }

        return poly
    }

    // Calculate straight-line distance (fallback if Directions API fails)
    fun calculateStraightLineDistance(origin: LatLng, destination: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            origin.latitude, origin.longitude,
            destination.latitude, destination.longitude,
            results
        )
        return results[0]
    }
}


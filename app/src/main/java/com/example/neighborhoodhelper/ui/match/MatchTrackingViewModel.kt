package com.example.neighborhoodhelper.ui.match

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neighborhoodhelper.data.FirebaseRepository
import com.example.neighborhoodhelper.model.ActiveMatch
import com.example.neighborhoodhelper.utils.DirectionsApiHelper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchTrackingViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _activeMatch = MutableStateFlow<ActiveMatch?>(null)
    val activeMatch: StateFlow<ActiveMatch?> = _activeMatch

    private val _routePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val routePolyline: StateFlow<List<LatLng>> = _routePolyline

    private val _isHelper = MutableStateFlow(false)
    val isHelper: StateFlow<Boolean> = _isHelper

    private var matchId: String = ""

    fun initialize(matchId: String) {
        this.matchId = matchId
        observeMatch(matchId)
    }

    private fun observeMatch(matchId: String) {
        viewModelScope.launch {
            repository.observeActiveMatch(matchId).collect { match ->
                Log.d("MatchTrackingVM", "🔄 Match update received: ${match?.status}")
                _activeMatch.value = match
                match?.let {
                    val currentUserId = auth.currentUser?.uid
                    _isHelper.value = it.helperId == currentUserId
                    Log.d("MatchTrackingVM", "👤 User role: ${if (_isHelper.value) "Helper" else "Requester"}")

                    // Fetch route when match updates
                    fetchRoute(it)
                }
            }
        }
    }

    private fun fetchRoute(match: ActiveMatch) {
        viewModelScope.launch {
            try {
                val origin = LatLng(match.helperLat, match.helperLon)
                val destination = LatLng(match.requesterLat, match.requesterLon)

                Log.d("MatchTrackingVM", "🗺️ Fetching route from ($origin) to ($destination)")

                val result = DirectionsApiHelper.getDirections(origin, destination)
                result.onSuccess { routeResult ->
                    _routePolyline.value = routeResult.polyline
                    Log.d("MatchTrackingVM", "✅ Route fetched: ${routeResult.polyline.size} points")
                }.onFailure { error ->
                    Log.e("MatchTrackingVM", "❌ Failed to get route: ${error.message}", error)
                    _routePolyline.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("MatchTrackingVM", "❌ Error fetching route", e)
                _routePolyline.value = emptyList()
            }
        }
    }

    fun updateMyLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val match = _activeMatch.value ?: return@launch
            val currentUserId = auth.currentUser?.uid ?: return@launch

            val isHelperUser = match.helperId == currentUserId

            // Calculate distance
            val origin = LatLng(latitude, longitude)
            val destination = if (isHelperUser) {
                LatLng(match.requesterLat, match.requesterLon)
            } else {
                LatLng(match.helperLat, match.helperLon)
            }

            val distance = DirectionsApiHelper.calculateStraightLineDistance(origin, destination)

            repository.updateMatchLocation(
                matchId = matchId,
                isHelper = isHelperUser,
                latitude = latitude,
                longitude = longitude,
                distance = distance
            )
        }
    }

    fun markProximityReached() {
        viewModelScope.launch {
            repository.updateMatchProximityReached(matchId)
        }
    }

    fun completeMatch() {
        viewModelScope.launch {
            val match = _activeMatch.value ?: return@launch
            repository.completeActiveMatch(matchId, match.postId)
        }
    }

    fun submitRating(rating: Float, review: String) {
        viewModelScope.launch {
            val match = _activeMatch.value ?: return@launch
            val currentUserId = auth.currentUser?.uid ?: return@launch
            val isHelperUser = match.helperId == currentUserId

            val toUserId = if (isHelperUser) match.requesterId else match.helperId

            repository.submitRating(
                postId = match.postId,
                toUserId = toUserId,
                rating = rating,
                review = review,
                category = "OTHER" // We'd need to get this from post
            )

            // Create task history
            val post = repository.getCurrentUser() // This is a simplified version
            // In reality, you'd fetch the actual post details
        }
    }
}


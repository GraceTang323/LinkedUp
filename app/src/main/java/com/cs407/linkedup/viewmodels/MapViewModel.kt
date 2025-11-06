package com.cs407.linkedup.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Represents a student with their name, subject, and location
// Can add more attributes (like a list of subjects and interests) once databases are set up
data class Student(
    val name: String,
    val major: String,
    val location: LatLng
)

data class MapState(
    // A list of markers currently displayed on the map
    val markers: List<LatLng> = emptyList(),
    // Stores the user's most recent location (if available)
    val currentLocation: LatLng? = null,
    // Tracks whether location permissions are granted
    val locationPermissionGranted: Boolean = false,
    // Indicates when location or map data is being loaded
    val isLoading: Boolean = false,
    // Stores any error msg that occurred during data loading
    val error: String? = null,
)

class MapViewModel: ViewModel() {
    // Backing property for state: MutableStateFlow lets us update data internally
    private val _uiState = MutableStateFlow(MapState())

    // Expose uiState as a read-only StateFlow to the UI layer
    val uiState = _uiState.asStateFlow()

    // Interacts with the Google Maps SDK to retrieve location data
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // TODO: Delete this and replace with "real" student data once database is set up
    private val _mockStudents = listOf(
        Student("John Doe", "Computer Science", LatLng(43.0731, -89.4052)),
        Student("Sam Smith", "Biochemical Engineering", LatLng(43.0736, -89.4066)),
        Student("Mary Harvey", "Nursing", LatLng(43.0720, -89.4050)),
        Student("Flint Lockwood", "Nursing", LatLng(43.0739, -89.4046)),
    )
    // List of fake students for map testing purposes
    val mockStudents: List<Student> get() = _mockStudents

    // Initializes the location client when a valid Context becomes available
    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        viewModelScope.launch @androidx.annotation.RequiresPermission(
            allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION]
        ) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            try {
                val state = _uiState.value

                // check if location permission is granted
                if (!state.locationPermissionGranted) {
                    _uiState.value = state.copy(
                        error = "Location permission not granted",
                        isLoading = false
                    )
                    return@launch
                }
                // retrieve the last known location
                val locationResult: Task<Location> = fusedLocationClient.lastLocation
                val location = locationResult.await()

                val currentLocation = LatLng(location.latitude, location.longitude)
                _uiState.value = state.copy(
                    currentLocation = currentLocation,
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Unexpected error occurred fetching current location",
                    isLoading = false
                )
            }
        }
    }

    // Updates the state to reflect whether location permissions are granted
    fun updateLocationPermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = granted
        )
    }
}
package com.cs407.linkedup.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.linkedup.repo.UserRepository
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
    val uid: String,
    val name: String,
    val major: String,
    val location: LatLng,
    val bio: String = "",
    val profilePictureUrl: String? = null // maybe use?
)

data class MapState(
    // A list of markers currently displayed on the map
    val markers: List<LatLng> = emptyList(),
    // Stores the user's most recent location (if available)
    val currentLocation: LatLng? = null,
    // Stores the user's chosen location
    val selectedLocation: LatLng? = null,
    // Tracks whether location permissions are granted
    val locationPermissionGranted: Boolean = false,
    // Indicates when location or map data is being loaded
    val isLoading: Boolean = false,
    // Stores any error msg that occurred during data loading
    val error: String? = null,
)

class MapViewModel(
    private val repository: UserRepository
): ViewModel() {
    // Backing property for state: MutableStateFlow lets us update data internally
    private val _uiState = MutableStateFlow(MapState())

    // Expose uiState as a read-only StateFlow to the UI layer
    val uiState = _uiState.asStateFlow()

    // Interacts with the Google Maps SDK to retrieve location data
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // list of nearby students to display
    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students = _students.asStateFlow()

    // Only true if both users express interest, false if one-way, null if no match yet
    private val _matchStatus = MutableStateFlow<Boolean?>(null)
    val matchStatus = _matchStatus.asStateFlow()

    init { // fetch nearby un-matched students immediately when MapViewModel is initialized
        viewModelScope.launch {
            repository.getNearbyStudents().collect { students ->
                val matchedIds = repository.getMatchedUserIds()
                val unmatched = students.filter{ it.uid !in matchedIds }
                _students.value = unmatched
            }
        }
    }

    var selectedStudent by mutableStateOf<Student?>(null)
        private set // read only variable

    fun selectStudent(student: Student) { // helper function to select a student
        selectedStudent = student
    }

    // Initializes the location client when a valid Context becomes available
    fun initializeLocationClient(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun setSelectedLocation(location: LatLng) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = location,
            error = null
        )
    }

    // Saves the location remotely to FireStore
    fun confirmSelectedLocation() {
        val selected = _uiState.value.selectedLocation ?: return
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                // upload to Firestore
                repository.saveUserLocation(selected)

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Retrieves the user's location from FireStore
    fun loadUserLocation() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val location = repository.getUserLocation()
                _uiState.value = _uiState.value.copy(
                    selectedLocation = location,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Clears the selected location
    fun resetSelection() {
        _uiState.value = _uiState.value.copy( selectedLocation = null )
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

    fun linkUp(targetUid: String) {
        viewModelScope.launch {
            val matched = repository.linkUp(targetUid)
            _matchStatus.value = matched
        }
    }
    // Cleans the match status for future matches
    fun clearMatch() {
        _matchStatus.value = null
    }
}
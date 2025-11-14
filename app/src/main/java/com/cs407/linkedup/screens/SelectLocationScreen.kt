package com.cs407.linkedup.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SelectLocationScreen(
    viewModel: MapViewModel,
    onLocationConfirm: (LatLng) -> Unit
) {
    // Automatically updates UI whenever data changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Gets a mutable state object that reflects the status of the location permission
    val locationPermissionState = rememberPermissionState(
        permission = Manifest.permission.ACCESS_FINE_LOCATION
    )
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // get current location, or request permission
    LaunchedEffect(locationPermissionState.status.isGranted) {
        // Check for permission status
        if (locationPermissionState.status.isGranted) {
            viewModel.updateLocationPermission(true)
            viewModel.initializeLocationClient(context)
            viewModel.getCurrentLocation()
        } else {
            // Request permission
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Sets Madison as the default starting camera location for the map
    val defaultLocation = LatLng(43.0731, -89.4012)

    // Remembers camera position state so it persists upon recompositions
    val cameraPositionState = rememberCameraPositionState {
        // Sets the initial position and zoom level of the map
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }

    // Set the camera to the user's current location only once upon launch
    LaunchedEffect(Unit) {
        uiState.currentLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                location,
                16f,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map screen
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedLocation = latLng
                viewModel.setSelectedLocation(latLng)
                // Move the camera to the selected location
                coroutineScope.launch {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLng(latLng)
                    )
                }
            }
        ) {
            selectedLocation?.let { location ->
                Marker (
                    state = MarkerState(position = location),
                    title = "Selected Location",
                    snippet = "${location.latitude}, ${location.longitude}"
                )
            }
        }

        // Header
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color.White.copy(alpha = 0.85f))
                .padding(top = 40.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {}, // disables clicking
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.location_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.location_body),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Confirm button
        selectedLocation?.let { location ->
            Button(
                onClick = {
                    viewModel.setSelectedLocation(location)
                    viewModel.confirmSelectedLocation()
                    onLocationConfirm(location)
                          },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(0.9f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Confirm Location")
            }
        }
    }
}
package com.cs407.linkedup.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cs407.linkedup.viewmodels.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel
) {
    // Automatically updates UI whenever data changes
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Define a default location for the map to load to before the user's location is obtained
    val defaultLocation = LatLng(43.0731, -89.4012)

    // Remembers camera position state so it persists upon recompositions
    val cameraPositionState = rememberCameraPositionState {
        // Sets the initial position and zoom level of the map
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }

    // Updates the camera position to reflect the user's selected location
    LaunchedEffect(uiState.selectedLocation) {
        viewModel.loadUserLocation()
        uiState.selectedLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                location,
                16f,
            )
        }
    }
    // The GoogleMap composable displays the map UI inside your Compose layout
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {

        LaunchedEffect(uiState.selectedLocation) {
            Log.d("MapScreen", "Selected Location: ${uiState.selectedLocation}, " +
                    "Error: ${uiState.error}")
        }

        // Display the user's chosen location on the map
        uiState.selectedLocation?.let { location ->
            MarkerComposable(
                state = MarkerState(position = location),
                content = {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xff209640),   // The inner circle is green
                                shape = CircleShape         // The marker shape is circular
                            )
                            .border(
                                width = 4.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    )
                }
            )
        }
        // Display nearby student markers
        viewModel.mockStudents.forEach { student ->
            MarkerComposable(
                state = MarkerState(position = student.location),
                content = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    Color(0xFF4285F4),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = student.name,
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    }
}

// Custom map marker, potentially use to display other students?
@Composable
fun MapMarker(
    imageUrl: String?,
    name: String,
    location: LatLng,
    onClick: () -> Unit
) {
    val markerState = remember { MarkerState(position = location) }
    val shape = RoundedCornerShape(
        20.dp, 20.dp,
        20.dp, 20.dp
    )
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .build()
    )

    MarkerComposable(
        keys = arrayOf(name, painter.state),
        state = markerState,
        title = name,
        anchor = Offset(0.5f, 1f),
        onClick = {
            onClick()
            true
        }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shape)
                .background(Color.LightGray)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                Image(
                    painter = painter,
                    contentDescription = "${name}'s Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
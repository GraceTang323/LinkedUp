package com.cs407.linkedup.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cs407.linkedup.R
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

    var showUserCard by remember { mutableStateOf(false) }

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
                onClick = {
                    showUserCard = true
                    true // click is consumed
                    },
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

    // TODO: fix user card sliding all the way to the screen's height
    // Displays the user card when showUserCard is true
    AnimatedVisibility(
        visible = showUserCard,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight }, // ensures the card starts offscreen bottom
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight } // ensures the card slides offscreen bottom
        ) + fadeOut()
    ) {
        UserCard(
            onCloseClick = { showUserCard = false }
        )
    }
}

@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit = {},
    // add a user repo object or similar to fetch user name, major, bio, etc.
) {
    Card(
        modifier = modifier
            .fillMaxWidth() // remove for testing?
            .padding(16.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) { // Box centers the icon button
            IconButton(
                onClick = { onCloseClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Hide User Card",
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // takes all remaining space
            ) {
                // Name
                Text(
                    text = "John Smith",
                    fontSize = 30.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                // Major(s)
                Text(
                    text = "Studying: Mathematics", // TODO: change to something like "studying: ${major}"
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
                // Bio, if provided
                Text(
                    text = "An English soldier, explorer, colonial governor, admiral of New England, and author. " +
                            "Currently studying calculus at Union South",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 8.dp)
                        .widthIn(max = 200.dp)
                )

                Button(
                    onClick = {
                        // TODO: implement linking up logic
                        onCloseClick()
                    },
                    colors = ButtonDefaults.buttonColors(Color(0xff209640)),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 16.dp, start = 16.dp)
                ) {
                    Icon( // change Link Up button icon if better icon is found
                        Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Link Up"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link UP")
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Image( // placeholder image
                    painter = painterResource(id = R.drawable.ic_launcher_background),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(bottom = 160.dp, end = 30.dp)
                        .size(100.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewUserCard() {
    UserCard()
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
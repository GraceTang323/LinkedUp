package com.cs407.linkedup.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cs407.linkedup.R
import com.cs407.linkedup.ui.theme.mintGreen
import com.cs407.linkedup.viewmodels.MapViewModel
import com.cs407.linkedup.viewmodels.SettingsViewModel
import com.cs407.linkedup.viewmodels.Student
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    settingsViewModel: SettingsViewModel
) {
    // Automatically updates UI whenever data changes
    val uiState by mapViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.settingsState.collectAsStateWithLifecycle()

    // Define a default location for the map to load to before the user's location is obtained
    val defaultLocation = LatLng(43.0731, -89.4012)

    // Remembers camera position state so it persists upon recompositions
    val cameraPositionState = rememberCameraPositionState {
        // Sets the initial position and zoom level of the map
        position = CameraPosition.fromLatLngZoom(defaultLocation, 16f)
    }
    // Converts Flow<List<Student>> to List<Student> so we can display them on the map
    val students by mapViewModel.students.collectAsState()
    val matches by mapViewModel.matchedStudents.collectAsState()
    val selectedStudent = mapViewModel.selectedStudent
  
    val studentsInRange =
        uiState.selectedLocation?.let { location ->
            students.filter { student ->
                SphericalUtil.computeDistanceBetween(student.location, location) / 1000.0 <= settingsState.searchRadius
            }
        } ?: emptyList<Student>()

    var showUserCard by remember { mutableStateOf(false) }
    var showMatchDialog by remember { mutableStateOf(false) }

    val isMatched by mapViewModel.matchStatus.collectAsState()
    val context = LocalContext.current

    // Updates the camera position to reflect the user's selected location
    LaunchedEffect(uiState.selectedLocation) {
        mapViewModel.loadUserLocation()
        uiState.selectedLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                location,
                16f,
            )
        }
    }

    LaunchedEffect(isMatched) {
        when (isMatched) {
            true -> {
                // it's a match
                showMatchDialog = true
                mapViewModel.clearMatch()
            }
            false -> {
                // not a match yet
                Toast.makeText(
                    context,
                    "You liked ${selectedStudent?.name}",
                    Toast.LENGTH_SHORT
                ).show()
                mapViewModel.clearMatch()
            }
            null -> Unit // no match, yet, do nothing
        }
    }
    // Displays the map UI
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
        studentsInRange.forEach { student ->
            // Use unique key for each marker so old markers are not recreated with every update
            key("${student.name}+${student.location}") {
                MarkerComposable(
                    state = MarkerState(position = student.location),
                    onClick = {
                        mapViewModel.selectStudent(student)
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
        matches.forEach { student ->
            key("${student.name}+${student.location}") {
                MarkerComposable(
                    state = MarkerState(position = student.location),
                    onClick = {
                        mapViewModel.selectStudent(student)
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
                                        color = mintGreen,
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
            isMatched = selectedStudent in matches,
            student = selectedStudent,
            onLinkUpClick = {
                Log.d("MapScreen", "Link Up button clicked")
                mapViewModel.linkUp(selectedStudent!!.uid)
                showUserCard = false
            },
            onCloseClick = { showUserCard = false }
        )
    }

    if (showMatchDialog) {
        MatchDialog(
            selectedStudent = selectedStudent,
            onStartTalking = { showMatchDialog = false }, // TODO: Navigate to chat screen
            onLater = { showMatchDialog = false }
        )
    }
}

@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    student: Student?,
    isMatched: Boolean = false,
    onLinkUpClick: () -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                    text = student?.name ?: "Empty Name",
                    fontSize = 30.sp,
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
                // Major(s)
                Text(
                    text = "Studying: ${student?.major ?: "Undecided"}",
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
                // Bio, if provided
                Text(
                    text = student?.bio ?: "Let's meet up!",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 8.dp)
                        .widthIn(max = 200.dp)
                )

                Button(
                    enabled = !isMatched, // disable link up button for already matched users
                    onClick = { onLinkUpClick() },
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

@Composable
fun MatchDialog(
    selectedStudent: Student?,
    onStartTalking: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        confirmButton = {},
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "It's a Link!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You and ${selectedStudent?.name} have similar interests!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // start talking button
                Button(
                    onClick = onStartTalking,
                    colors = ButtonDefaults.buttonColors(Color(0xff209640)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Talking")
                }
                Spacer(modifier = Modifier.height(10.dp))

                // maybe later button
                Button(
                    onClick = onLater,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe Later")
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Preview
@Composable
fun PreviewUserCard() {
    UserCard(student = null)
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
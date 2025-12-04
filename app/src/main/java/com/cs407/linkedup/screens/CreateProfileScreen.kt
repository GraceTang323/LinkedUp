package com.cs407.linkedup.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel
import com.cs407.linkedup.viewmodels.PhotoViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Main screen for creating a user profile
 * Handles photo uploads, personal information input, and permission requests
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateProfileScreen(
    viewModel: AuthViewModel = viewModel(),
    photoViewModel: PhotoViewModel = viewModel(),
    onCreateProfileSuccess: () -> Unit,
) {
    // Collect state from ViewModels
    val authState by viewModel.authState.collectAsState()
    val photoState by photoViewModel.photoState.collectAsState()

    // Local state for form fields
    var name by remember { mutableStateOf("") }
    var major by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Load photo from Firebase when screen is displayed
    LaunchedEffect(Unit) {
        photoViewModel.loadProfilePhoto()
    }

    // Update local imageUri when photo is loaded from Firebase
    LaunchedEffect(photoState.photoUrl) {
        if (photoState.photoUrl != null && imageUri == null) {
            imageUri = Uri.parse(photoState.photoUrl)
        }
    }

    // Determine correct permission based on Android version
    // Android 13+ uses READ_MEDIA_IMAGES, older versions use READ_EXTERNAL_STORAGE
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Permission state handler
    val permissionState = rememberPermissionState(permission = permissionToRequest)

    // Image picker launcher
    // When user selects an image, it immediately uploads to Firebase
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Upload photo to Firebase Storage immediately
            photoViewModel.uploadProfilePhoto(it)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header text
            headerText()

            // Display profile picture
            // Priority: local selected image > uploaded image from Firebase
            val displayUri = imageUri ?: photoState.photoUrl?.let { Uri.parse(it) }
            profilePicture(displayUri)

            // Upload progress indicator
            if (photoState.isUploading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(280.dp)
                ) {
                    LinearProgressIndicator(
                        progress = photoState.uploadProgress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uploading: ${photoState.uploadProgress.toInt()}%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Change picture button with permission handling
            changePictureButton(
                onButtonClick = {
                    when {
                        // Permission already granted - open picker
                        permissionState.status.isGranted -> {
                            launcher.launch("image/*")
                        }
                        // Should show rationale - explain why we need permission
                        permissionState.status.shouldShowRationale -> {
                            permissionState.launchPermissionRequest()
                        }
                        // First time requesting permission
                        else -> {
                            permissionState.launchPermissionRequest()
                        }
                    }
                },
                hasPhotoAccess = { permissionState.status.isGranted },
                requestPhotoAccess = { permissionState.launchPermissionRequest() }
            )

            // Form fields
            PhoneNumberField(phoneNumber) { phoneNumber = it }
            nameTextField(name) { name = it }
            majorTextField(major) { major = it }
            bioTextField(bio) { bio = it }

            // Error messages from AuthViewModel
            if (authState.error != null) {
                Text(
                    text = authState.error ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Error messages from PhotoViewModel
            if (photoState.error != null) {
                Text(
                    text = photoState.error ?: "",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Next button - validates and saves profile
            nextButton {
                if (name.isNotBlank() && major.isNotBlank() && phoneNumber.isNotBlank()) {
                    viewModel.saveProfile(name, major, bio, stringifyPhoneNumber(phoneNumber))
                    onCreateProfileSuccess()
                }
            }
        }
    }
}

/**
 * Displays the "Create Profile" header text
 */
@Composable
fun headerText() {
    Text(
        stringResource(id = R.string.create_profile),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Displays circular profile picture
 * Shows placeholder icon if no image is provided
 *
 * @param imageUri URI of the image to display (can be local or from Firebase)
 */
@Composable
fun profilePicture(imageUri: Uri?) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            // Display actual image
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Display placeholder icon
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Placeholder Image",
                modifier = Modifier.size(100.dp),
                tint = Color.Gray
            )
        }
    }
}

/**
 * Button to change profile picture
 * Handles permission checking and image selection
 *
 * @param onButtonClick Action when button is clicked
 * @param hasPhotoAccess Function to check if app has photo access permission
 * @param requestPhotoAccess Function to request photo access permission
 */
@Composable
fun changePictureButton(
    onButtonClick: () -> Unit,
    hasPhotoAccess: () -> Boolean,
    requestPhotoAccess: () -> Unit
) {
    Button(
        onClick = onButtonClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(stringResource(id = R.string.change_picture))
    }
}

/**
 * Text field for user's name input
 *
 * @param name Current name value
 * @param onNameChange Callback when name changes
 */
@Composable
fun nameTextField(
    name: String,
    onNameChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(id = R.string.name_label)) },
        placeholder = {
            Text(
                stringResource(id = R.string.name_placeholder),
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Person,
                contentDescription = "Name",
                tint = Color.LightGray
            )
        },
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

/**
 * Text field for user's major input
 *
 * @param major Current major value
 * @param onMajorChange Callback when major changes
 */
@Composable
fun majorTextField(
    major: String,
    onMajorChange: (String) -> Unit
) {
    OutlinedTextField(
        value = major,
        onValueChange = onMajorChange,
        label = { Text(stringResource(id = R.string.major_label)) },
        placeholder = {
            Text(
                stringResource(id = R.string.major_placeholder),
                color = Color.Gray
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.School,
                contentDescription = "Major",
                tint = Color.LightGray
            )
        },
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

/**
 * Multi-line text field for user's bio
 *
 * @param bio Current bio value
 * @param onBioChange Callback when bio changes
 */
@Composable
fun bioTextField(
    bio: String,
    onBioChange: (String) -> Unit
) {
    OutlinedTextField(
        value = bio,
        onValueChange = onBioChange,
        label = { Text(stringResource(id = R.string.bio_label)) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Bio Icon",
                    tint = Color.LightGray
                )
            }
        },
        placeholder = {
            Text(
                stringResource(id = R.string.bio_placeholder),
                color = Color.Gray
            )
        },
        singleLine = false,
        modifier = Modifier
            .width(280.dp)
            .height(160.dp)
    )
}

/**
 * Converts phone number digits to storage format
 * Adds +1 country code for US numbers (10+ digits)
 *
 * @param digits Raw phone number digits
 * @return Formatted phone number string for storage
 */
fun stringifyPhoneNumber(digits: String): String {
    val filtered = digits.filter { it.isDigit() }
    return if (filtered.length >= 10) "+1$filtered" else "+$filtered"
}

/**
 * Formats phone number for UI display
 * Format: (XXX) XXX-XXXX
 *
 * @param digits Raw phone number digits
 * @return Formatted phone number string for display
 */
fun formatPhoneNumber(digits: String): String {
    return if (digits.length >= 10) {
        "(${digits.take(3)}) ${digits.substring(3, 6)}-${digits.substring(6, 10)}"
    } else {
        digits
    }
}

/**
 * Text field for phone number input
 * Automatically formats input as (XXX) XXX-XXXX
 *
 * @param number Current phone number value
 * @param onNumberChange Callback when number changes
 */
@Composable
fun PhoneNumberField(
    number: String,
    onNumberChange: (String) -> Unit
) {
    OutlinedTextField(
        value = number,
        onValueChange = { input ->
            // Filter to digits only and format
            val digits = input.filter { it.isDigit() }
            val formatted = formatPhoneNumber(digits)
            onNumberChange(formatted)
        },
        leadingIcon = {
            Icon(
                Icons.Default.Phone,
                contentDescription = "Phone",
                tint = Color.LightGray
            )
        },
        label = { Text("Cell Number") },
        singleLine = true,
        modifier = Modifier.width(280.dp),
    )
}

/**
 * Next button to proceed to next step
 * Validates and saves profile information
 *
 * @param onButtonClick Action when button is clicked
 */
@Composable
fun nextButton(
    onButtonClick: () -> Unit
) {
    Button(
        onClick = onButtonClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.next))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
        }
    }
}
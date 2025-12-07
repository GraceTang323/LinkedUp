package com.cs407.linkedup.screens

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel
import com.cs407.linkedup.viewmodels.PhotoViewModel
import com.cs407.linkedup.viewmodels.ProfileViewModel
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
    viewModel: AuthViewModel,
    photoViewModel: PhotoViewModel = viewModel(),
    onCreateProfileSuccess: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    // Collect state from ViewModels
    val authState by viewModel.authState.collectAsState()
    val photoState by photoViewModel.photoState.collectAsState()
    val context = LocalContext.current

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

    // Clear local URI when upload completes
    LaunchedEffect(photoState.photoBase64) {
        if (photoState.photoBase64 != null && !photoState.isUploading) {
            imageUri = null
        }
    }

    // Determine correct permission based on Android version
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Permission state handler
    val permissionState = rememberPermissionState(permission = permissionToRequest)

    // Image picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            // Upload photo as Base64 to Firestore
            photoViewModel.uploadProfilePhoto(it, context)
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

            // Display profile picture with Base64 support
            profilePicture(
                imageUri = imageUri,
                base64String = photoState.photoBase64
            )

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
                        permissionState.status.isGranted -> {
                            launcher.launch("image/*")
                        }
                        permissionState.status.shouldShowRationale -> {
                            permissionState.launchPermissionRequest()
                        }
                        else -> {
                            permissionState.launchPermissionRequest()
                        }
                    }
                },
                hasPhotoAccess = { permissionState.status.isGranted },
                requestPhotoAccess = { permissionState.launchPermissionRequest() }
            )

            // Form fields
            PhoneNumberField(phoneNumber,{ phoneNumber = it })
            nameTextField(name, { name = it })
            majorTextField(major, { major = it } )
            bioTextField(bio, { bio = it } )

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
                    profileViewModel.loadProfile(authState.currentUser?.uid)
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
 * Supports both local URI (immediate preview) and Base64 (from Firestore)
 *
 * @param imageUri Local URI of the image (for immediate preview)
 * @param base64String Base64 encoded image from Firestore
 */
@Composable
fun profilePicture(imageUri: Uri? = null, base64String: String? = null) {
    // Decode Base64 outside of Composable scope to avoid try-catch issues
    val decodedBitmap = remember(base64String) {
        if (base64String != null && base64String.isNotEmpty()) {
            try {
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Priority 1: Show local image (immediate preview)
            imageUri != null -> {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Priority 2: Show Base64 image from Firestore
            decodedBitmap != null -> {
                Image(
                    bitmap = decodedBitmap.asImageBitmap(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Priority 3: Show placeholder
            else -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Placeholder Image",
                    modifier = Modifier.size(100.dp),
                    tint = Color.Gray
                )
            }
        }
    }
}

/**
 * Button to change profile picture
 * Handles permission checking and image selection
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
 */
@Composable
fun nameTextField(
    name: String,
    onNameChange: (String) -> Unit,
    isEditing: Boolean = true
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        readOnly = !isEditing,
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
 */
@Composable
fun majorTextField(
    major: String,
    onMajorChange: (String) -> Unit,
    isEditing: Boolean = true
) {
    OutlinedTextField(
        value = major,
        onValueChange = onMajorChange,
        readOnly = !isEditing,
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
 */
@Composable
fun bioTextField(
    bio: String,
    onBioChange: (String) -> Unit,
    isEditing: Boolean = true
) {
    OutlinedTextField(
        value = bio,
        onValueChange = onBioChange,
        readOnly = !isEditing,
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
 */
fun stringifyPhoneNumber(digits: String): String {
    val filtered = digits.filter { it.isDigit() }
    return if (filtered.length >= 10) "+1$filtered" else "+$filtered"
}

/**
 * Formats phone number for UI display
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
 */
@Composable
fun PhoneNumberField(
    number: String,
    onNumberChange: (String) -> Unit,
    isEditing: Boolean = true
) {
    OutlinedTextField(
        value = number,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            val formatted = formatPhoneNumber(digits)
            onNumberChange(formatted)
        },
        readOnly = !isEditing,
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
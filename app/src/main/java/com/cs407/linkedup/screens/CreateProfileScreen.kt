package com.cs407.linkedup.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun headerText(){
    Text( stringResource(id = R.string.create_profile),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}


@Composable
fun profilePicture(imageUri: Uri?){
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color.LightGray),
        contentAlignment = Alignment.Center

    ){
        if(imageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(model = imageUri),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else{
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Placeholder Image",
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
fun changePictureButton(
    onButtonClick: () -> Unit,
    hasPhotoAccess: () -> Boolean,
    requestPhotoAccess: () -> Unit
){
    var hasAccess = hasPhotoAccess()

    Button(
        onClick = if(hasAccess){
            onButtonClick
        } else {
            requestPhotoAccess()
            onButtonClick
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ){
        Text(stringResource(id = R.string.change_picture))
    }
}

@Composable
fun nameTextField(
    name: String,
    onNameChange: (String) -> Unit
){
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(id = R.string.name_label)) },
        placeholder = { Text(
            stringResource(id = R.string.name_placeholder),
            color = Color.Gray )
            },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color.LightGray)},
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

@Composable
fun majorTextField(
    major: String,
    onMajorChange: (String) -> Unit
){
    OutlinedTextField(
        value = major,
        onValueChange = onMajorChange,
        label = { Text(stringResource(id = R.string.major_label)) },
        placeholder = { Text(
            stringResource(id = R.string.major_placeholder),
            color = Color.Gray )
        },
        leadingIcon = { Icon(Icons.Default.School, contentDescription = "Major", tint = Color.LightGray)},
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

@Composable
fun bioTextField(
    bio: String,
    onBioChange: (String) -> Unit
){
    OutlinedTextField(
        value = bio,
        onValueChange = onBioChange,
        label = { Text(stringResource(id = R.string.bio_label)) },
        leadingIcon = { Box(
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
        }},
        placeholder = { Text(
            stringResource(id = R.string.bio_placeholder),
            color = Color.Gray )
        },
        singleLine = false,
        modifier = Modifier.width(280.dp).height(160.dp)
    )
}

// Format phone number into a string for storage
fun stringifyPhoneNumber(digits: String): String {
    val filtered = digits.filter { it.isDigit() }
    return if (filtered.length >= 10) "+1$filtered" else "+$filtered"
}

// Format phone number for UI display
fun formatPhoneNumber(digits: String): String {
    if (digits.length >= 10) {
        return "(${digits.take(3)}) ${digits.substring(3,6)}-${digits.substring(6,10)}"
    } else {
        return digits
    }
}

@Composable
fun PhoneNumberField(
    number: String,
    onNumberChange: (String) -> Unit
) {
    OutlinedTextField(
        value = number,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            val formatted = formatPhoneNumber(digits)
            onNumberChange(formatted)
        },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone", tint = Color.LightGray)},
        label = { Text("Cell Number") },
        singleLine = true,
        modifier = Modifier.width(280.dp),
    )
}

@Composable
fun nextButton(
    onButtonClick: () -> Unit
){
    Button(
        onClick = onButtonClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )

    ){
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(id = R.string.next))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")

        }
    }
}
@Composable
fun CreateProfileScreen(
    viewModel: AuthViewModel = viewModel(),
    hasPhotoAccess: () -> Boolean,
    requestPhotoAccess: () -> Unit,
    onCreateProfileSuccess: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()

    var name by remember{ mutableStateOf("") }
    var major by remember{ mutableStateOf("") }
    var bio by remember{ mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    //Variable for the user chosen image
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    //Launcher that will prompt the user to choose an image when launcher.launch is called
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(){ innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)

        ){
            headerText()
            profilePicture(imageUri)
            changePictureButton(
                onButtonClick = { launcher.launch("image/*") },
                hasPhotoAccess = hasPhotoAccess,
                requestPhotoAccess = requestPhotoAccess
                )
            PhoneNumberField(phoneNumber, { input -> phoneNumber = input })
            nameTextField(name, { input -> name = input })
            majorTextField(major, { input -> major = input })
            bioTextField(bio, { input -> bio = input })

            // errors, if any
            if (authState.error != null) {
                Text(
                    text = authState.error ?: "",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            nextButton(onButtonClick = {
                // Add name, major, and bio to FireStore
                if (authState.error == null && name != "" && major != "" && phoneNumber != "") {
                    viewModel.saveProfile(name, major, bio, stringifyPhoneNumber(phoneNumber))
                    onCreateProfileSuccess()
                }
            })
        }
    }
}
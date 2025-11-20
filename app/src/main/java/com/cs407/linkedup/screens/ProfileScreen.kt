package com.cs407.linkedup.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel
import com.cs407.linkedup.viewmodels.ProfileViewModel

@Composable
fun logoutButton(
    viewModel: AuthViewModel,
    onLogout: () -> Unit
){
    // Logout Button
    Button(
        onClick = {
            viewModel.logout()
            onLogout()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32), // darker green?
            contentColor = Color.White
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(text = stringResource(R.string.logout_button))
    }
}

@Composable
fun deleteAccountButton(
    onDeleteClick: () -> Unit
){
    // Delete Account Button
    OutlinedButton(
        onClick = onDeleteClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.Red
        ),
        border = BorderStroke(1.dp, Color.Red),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Account Icon",
            tint = Color.Red
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.delete_button),
            color = Color.Red
        )
    }
}

@Composable
fun prefButton(
    viewModel: ProfileViewModel,
    onPrefClick: () -> Unit
){
    // Logout Button
    Button(
        onClick = {
            onPrefClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF2E7D32), // darker green?
            contentColor = Color.White
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(text = "Preferences")
    }
}

@Composable

fun saveProfileButton(
    profileViewModel: ProfileViewModel,
    phoneNumber: String,
    name: String,
    major: String,
    bio: String
){
    val context = LocalContext.current
    Button(
        onClick = {
            profileViewModel.updateProfile(phoneNumber, name, major, bio )
            //If there is an error, make that the toast message, otherwise, show success toast
            if(profileViewModel.profileState.value.error != null){
                Toast.makeText(context, profileViewModel.profileState.value.error, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Successfully saved changes!", Toast.LENGTH_LONG).show()
            }
                  },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ){
        Text(stringResource( id = R.string.save_profile) )
    }
}
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    hasPhotoAccess: () -> Boolean,
    requestPhotoAccess: () -> Unit,
    onLogout: () -> Unit,
    onDelete: () -> Unit,
    onPrefClick: () -> Unit,
) {
    val authState by authViewModel.authState.collectAsState()
    val profileState by profileViewModel.profileState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    var name by remember{ mutableStateOf(profileState.name) }
    var major by remember{ mutableStateOf(profileState.major) }
    var bio by remember{ mutableStateOf(profileState.bio) }
    var phoneNumber by remember { mutableStateOf(profileState.phoneNumber) }



    //TODO: Placeholder variable, this should eventually be obtained from a viewmodel
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    //Launcher that will prompt the user to choose an image when launcher.launch is called
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold { innerPadding ->
            //These functions are defined in CreateProfileScreen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(2.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                profilePicture(imageUri)
                Row() {
                    changePictureButton(
                        onButtonClick = { launcher.launch("image/*") },
                        hasPhotoAccess = hasPhotoAccess,
                        requestPhotoAccess = requestPhotoAccess
                    )
                    saveProfileButton(
                        profileViewModel = profileViewModel,
                        phoneNumber = phoneNumber,
                        name = name,
                        major = major,
                        bio = bio
                    )
                    prefButton(
                        viewModel = profileViewModel,
                        onPrefClick = onPrefClick
                    )
                }
                PhoneNumberField(
                    phoneNumber,
                    { input -> phoneNumber = input }
                )
                nameTextField(name, { input -> name = input })
                majorTextField(major, { input -> major = input })
                bioTextField(bio, { input -> bio = input })
                Row() {
                    logoutButton(authViewModel, onLogout)
                    deleteAccountButton(onDeleteClick = { showDeleteDialog = true })
                }
            }

            // error message, if any
            if (authState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authState.error ?: "",
                    color = Color.Red,
                    fontSize = 14.sp
                )
            }

            if (showDeleteDialog) {
                AlertDeleteDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    onConfirm = {
                        authViewModel.deleteAccount()
                        onDelete()
                        showDeleteDialog = false
                    },
                    dialogTitle = stringResource(R.string.delete_title),
                    dialogText = stringResource(R.string.delete_text)
                )
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(stringResource(R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}
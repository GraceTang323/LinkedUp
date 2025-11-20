package com.cs407.linkedup.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel
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
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(vertical = 8.dp)
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
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .padding(vertical = 8.dp)
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
fun ProfileScreen(
    viewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit,
    onDelete: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    //TODO: Placeholder variable, this should eventually be obtained from a viewmodel
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            logoutButton(viewModel, onLogout)
            deleteAccountButton( onDeleteClick = { showDeleteDialog = true })

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
                        viewModel.deleteAccount()
                        onDelete()
                        showDeleteDialog = false
                    },
                    dialogTitle = stringResource(R.string.delete_title),
                    dialogText = stringResource(R.string.delete_text)
                )
            }
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
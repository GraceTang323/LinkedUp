package com.cs407.linkedup.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.linkedup.viewmodels.AuthViewModel

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit,
    onDelete: () -> Unit,
) {
    val authState by viewModel.authState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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
                Text("Log Out")
            }

            // Delete Account Button
            OutlinedButton(
                onClick = {
                    viewModel.deleteAccount()
                    onDelete()
                },
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
                    contentDescription = "Delete Account",
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Account",
                    color = Color.Red
                )
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
        }
    }
}
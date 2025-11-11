package com.cs407.linkedup.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel

@Composable
fun pageTitle(){
    Text(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        text = stringResource(id = R.string.create_button),
    )
}
@Composable
fun emailField(
    email: String,
    onEmailChange: (String) -> Unit,
    enabled: Boolean = true
){
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(id = R.string.email_label)) },
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

@Composable
fun passwordField(
    password: String,
    onPasswordChange: (String) -> Unit
){
    var passwordVisible by remember { mutableStateOf(false)}
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(id = R.string.password_label)) },
        singleLine = true,
        modifier = Modifier.width(280.dp),
        visualTransformation = if(!passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Default.Visibility
            else
                Icons.Default.VisibilityOff
            IconButton( onClick = { passwordVisible = !passwordVisible } ){
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password" )
            }
        }
    )
}

@Composable
fun passwordConfirmField(
    passwordConfirm: String,
    onPasswordConfirmChange: (String) -> Unit
){
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = passwordConfirm,
        onValueChange = onPasswordConfirmChange,
        label = { Text(stringResource(id = R.string.password_label)) },
        singleLine = true,
        modifier = Modifier.width(280.dp),
        visualTransformation = if(!passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Default.Visibility
            else
                Icons.Default.VisibilityOff
            IconButton( onClick = { passwordVisible = !passwordVisible } ){
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password" )
            }
        }
    )
}

@Composable
fun AccountButton(
    onCreateAccountClick: () -> Unit
){
    Button(
        onClick = onCreateAccountClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Green,
            contentColor = Color.Black
        )
    ){
        Text( text = stringResource(id = R.string.create_button) )
    }
}
@Composable
fun CreateAccountScreen(
    viewModel: AuthViewModel = viewModel(),
    onCreateAccountSuccess: () -> Unit,
    onBackClick: () -> Unit
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    // observe when Firebase user successfully registers
    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser != null) {
            onCreateAccountSuccess()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            verticalArrangement = spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Spacer(modifier = Modifier.height(200.dp))
            pageTitle()
            emailField(email, { input -> email = input })
            passwordField(password, { input -> password = input })
            passwordConfirmField(passwordConfirm, { input -> passwordConfirm = input })
            Text(
                text = stringResource(R.string.back_to_login),
                color = Color.Blue,
                modifier = Modifier.clickable { onBackClick() }
            )

            // errors, if any
            if (authState.error != null) {
                Text(
                    text = authState.error ?: "",
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            // loading indicator while signing in?
            if (authState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            AccountButton(onCreateAccountClick = {
                viewModel.createAccount(email, password, passwordConfirm)
            })

        }
    }
}
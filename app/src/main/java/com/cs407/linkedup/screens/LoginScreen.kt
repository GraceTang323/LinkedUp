package com.cs407.linkedup.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cs407.linkedup.R
import com.cs407.linkedup.viewmodels.AuthViewModel

@Composable
fun appTitle(){
    Text(
        buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                append("LINKED")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)){
                append("UP")
            }
        },
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun signInText(){
    Text(
        text = stringResource(id = R.string.sign_in),
        fontSize = 20.sp
    )
}
@Composable
fun userEmail(
    email: String,
    onEmailChange: (String) -> Unit
    ){
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(text = stringResource(id = R.string.email_label ))},
        singleLine = true,
        modifier = Modifier.width(280.dp)
    )
}

@Composable
fun userPassword(
    password: String,
    onPasswordChange: (String) -> Unit
){
    var passwordVisible by remember { mutableStateOf(false)}
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text( text = stringResource(id = R.string.password_label ))},
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
fun createAccountPrompt(
    onCreateAccountClick: () -> Unit
    //This will need onCreateAttempt() as a param
) {
    Row() {
        Text(
            text = stringResource(id = R.string.account_prompt),

        )
        Text(
            text = stringResource(id = R.string.create_prompt),
            color = Color(0xFF1E88E5),
            modifier = Modifier.clickable { onCreateAccountClick() }
        )
    }
}

@Composable
fun LoginButton(
    onLoginClick: () -> Unit
    //This function will need onLoginAttempt() as a param
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ),
        onClick = onLoginClick,

    ){
        Text(
            text = stringResource(id = R.string.login_button)
        )
    }
}
@Composable
fun LoginScreen (
    onCreateAccountClick: () -> Unit,
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("")}

    val authState by viewModel.authState.collectAsState() // observe auth state changes

    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess && authState.currentUser != null) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = spacedBy(10.dp)
        ) {
            appTitle()
            Spacer(modifier = Modifier.height(60.dp))
            signInText()
            Spacer(modifier = Modifier.height(20.dp))
            userEmail(email, { input -> email = input })
            userPassword(password, { input -> password = input } )

            // error message if any
            if (authState.error != null) {
                Text(
                    text = authState.error ?: "",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            // loading indicator while signing in?
            if (authState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            createAccountPrompt(onCreateAccountClick)
            Spacer(modifier = Modifier.height(5.dp))
            // Login button
            LoginButton(onLoginClick = {
                viewModel.login(email, password)
            })
        }
    }
}
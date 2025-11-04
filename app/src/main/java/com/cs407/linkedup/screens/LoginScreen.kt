package com.cs407.linkedup.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cs407.linkedup.R
@Composable
fun appTitle(){
    Text(
        buildAnnotatedString {
            append("LINKED")
            withStyle(style = SpanStyle(color = Color.Green)){
                append("UP")
            }
        }
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
                Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show pasword" )
            }
        }
    )
}
@Composable
fun LoginScreen (
    // This function should take the following parameters, but I'm not sure how we want to implement them
//    onLoginClick: () -> Unit,
//    onRegisterClick: () -> Unit
){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("")}
    var error by remember { mutableStateOf("") }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            appTitle()
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = "SIGN IN"
            )
            Spacer(modifier = Modifier.height(20.dp))
            userEmail(email, { input -> email = input })
            userPassword(password, { input -> password = input })




        }
    }
}
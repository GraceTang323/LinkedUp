package com.cs407.linkedup.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cs407.linkedup.R

@Composable
fun headerText(){
    Text( stringResource(id = R.string.create_profile) )
}

@Composable
fun CreateProfileScreen(){
    Scaffold(){ innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ){
            headerText()
        }
    }
}
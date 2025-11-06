package com.cs407.linkedup.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.cs407.linkedup.ui.theme.gray
import com.cs407.linkedup.ui.theme.mintGreen
import com.cs407.linkedup.viewmodels.AuthViewModel


@Composable
fun interestBox(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf(false) }
    var background = gray
    Surface(
        onClick = {
            selected = !selected
            background = if (selected) mintGreen else gray
            onClick()
        },
        color = background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color.Black),

        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(10.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreferencesScreen(
    viewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBackClick){
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = { BottomAppBar(

        ){
            Button(onClick = {
            }) {
                Text(text = "Save")
            }
        } }
    ) { innerPadding ->


    }
}
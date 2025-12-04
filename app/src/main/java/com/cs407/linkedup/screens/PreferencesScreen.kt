package com.cs407.linkedup.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.cs407.linkedup.data.Preferences
import com.cs407.linkedup.ui.theme.gray
import com.cs407.linkedup.ui.theme.mintGreen
import com.cs407.linkedup.viewmodels.AuthViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cs407.linkedup.ui.theme.black
import com.cs407.linkedup.viewmodels.ProfileViewModel

@Composable
fun InterestBox(
    text: String,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = {
            onClick()
        },
        color = if (selected) mintGreen else gray,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color.Black),

        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.DarkGray,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreferencesScreen(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    profileViewModel: ProfileViewModel
) {
    val profileState by profileViewModel.profileState.collectAsState()
//    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
   // var selectedClasses by remember { mutableStateOf(setOf<String>()) }

    var selectedInterests by remember(profileState.preferences.interests) {
        mutableStateOf(profileState.preferences.interests.toSet())
    }

    var selectedClasses by remember(profileState.preferences.classes) {
        mutableStateOf(profileState.preferences.classes.toSet())
    }

    Log.d("prefs", selectedInterests.toString())
    Log.d("prefs", selectedClasses.toString())



    fun toggleInterest(interest: String) {
        selectedInterests = if (selectedInterests.contains(interest)) {
            selectedInterests - interest
        } else {
            if (selectedInterests.size < 5) {
                selectedInterests + interest
            } else {
                selectedInterests
            }
        }
    }

    fun toggleClass(cls: String) {
        selectedClasses = if (selectedClasses.contains(cls)) {
            selectedClasses - cls
        } else {
            selectedClasses + cls
        }
    }




    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Profile Set Up",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0),
                modifier = Modifier.height(56.dp)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            profileViewModel.updatePreferences(selectedInterests.toList(), selectedClasses.toList())
                            onSaveClick()
                        },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Choose your\nareas of interest",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 34.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Max: 5",
                fontSize = 16.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Preferences.categoryToInterests.forEach { (category, interests) ->
                    item {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(interests) { interest ->
                                InterestBox(
                                    text = interest,
                                    selected = selectedInterests.contains(interest),
                                    onClick = { toggleInterest(interest) }
                                )
                            }
                        }
                    }
                }
                Preferences.classes.forEach { (category, interests) ->
                    item {
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            color = black,
                            fontStyle = FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(interests) { interest ->
                                InterestBox(
                                    text = interest,
                                    selected = selectedClasses.contains(interest),
                                    onClick = { toggleClass(interest) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

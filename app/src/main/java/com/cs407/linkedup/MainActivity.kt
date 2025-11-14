package com.cs407.linkedup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cs407.linkedup.repo.UserRepository
import com.cs407.linkedup.screens.MainScreen
import com.cs407.linkedup.ui.theme.LinkedUpTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore

        val repository = UserRepository(db, auth)

        enableEdgeToEdge()
        setContent {
            LinkedUpTheme {
                MainScreen(repository = repository)
            }
        }
    }
}
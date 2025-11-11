package com.cs407.linkedup.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.linkedup.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AuthViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val authState by viewModel.authState.collectAsState()

    val startDestination = if (authState.currentUser == null) "login" else "home"
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination?.route

    // determines whether to show the top and bottom tab bars (hides for register/login screens)
    val showTopBottomBar = currentRoute in listOf("home", "chat", "settings", "profile")

    // dynamically sets the title based on the current screen
    val title = when (currentRoute) {
        "home" -> "Home"
        "chat" -> "Chats"
        "settings" -> "Settings"
        "profile" -> "Profile"
        else -> "LinkedUp"
    }

    LaunchedEffect(authState.currentUser) {
        if (authState.currentUser == null) {
            navController.navigate("login") {
                popUpTo(0) // clear back stack
            }
        } else {
            navController.navigate("home") {
                popUpTo(0) // clear back stack
            }
        }
    }

    Scaffold(
        topBar = {
            if (showTopBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            if (showTopBottomBar) {
                BottomNavBar(
                    selectedTab = currentRoute ?: "home",
                    onTabSelected = { route ->
                        navController.navigate(route)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    onCreateAccountClick = { navController.navigate("create_account") },
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("create_account") {
                CreateAccountScreen(
                    onCreateAccountSuccess = { navController.navigate("create_profile") }
                )
            }
            composable("create_profile") {
                CreateProfileScreen(
                    //TODO: PLACEHOLDER FUNCTIONS MUST BE REPLACED
                    hasPhotoAccess = { true },
                    requestPhotoAccess = { },
                    onNextButtonClick = { navController.navigate("preferences_screen") }
                )
            }
            composable("preferences_screen") {
                PreferencesScreen(
                    onBackClick = {navController.navigate("create_profile") },
                    onSaveClick = { navController.navigate("home") }
                )
            }
            composable("home") { MapScreen() }
            composable("chat") { ChatsScreenPlaceholder() }
            composable("settings") { SettingsScreenPlaceholder() }
            composable("profile") {
                ProfileScreen(
                    onLogout = { navController.navigate("login") },
                    onDelete = { navController.navigate("login") }
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
) {
    NavigationBar(
        containerColor = Color.White
    ) {
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(
                Icons.Filled.Home,
                contentDescription = "Map Home",
            ) },
            label = { Text("Home")}
        )
        NavigationBarItem(
            selected = selectedTab == "chat",
            onClick = { onTabSelected("chat") }, // change icon later
            icon = { Icon(
                Icons.Filled.Notifications,
                contentDescription = "Chats",
            ) },
            label = { Text("Chat")}
        )
        NavigationBarItem(
            selected = selectedTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(
                Icons.Filled.Settings,
                contentDescription = "Settings",
            ) },
            label = { Text("Settings")}
        )
        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = { onTabSelected("profile") },
            icon = { Icon(
                Icons.Filled.Person,
                contentDescription = "Profile",
            ) },
            label = { Text("Profile")}
        )
    }
}

// Temporary placeholder screens, remove functions once actual screens are implemented
@Composable
fun ChatsScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Chats Screen Coming Soon")
    }
}

@Composable
fun SettingsScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Settings Screen Coming Soon")
    }
}

@Composable
fun ProfileScreenPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Profile Screen Coming Soon")
    }
}
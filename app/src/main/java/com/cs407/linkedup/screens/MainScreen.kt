package com.cs407.linkedup.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cs407.linkedup.repo.UserRepository
import com.cs407.linkedup.viewmodels.AuthViewModel
import com.cs407.linkedup.viewmodels.MapViewModel
import com.cs407.linkedup.viewmodels.ProfileViewModel
import com.cs407.linkedup.viewmodels.SettingsViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.authState.collectAsState()

    val repository = remember(authState.currentUser?.uid) {
        UserRepository(
            FirebaseFirestore.getInstance(),
            FirebaseAuth.getInstance()
        )
    }

    val mapViewModel: MapViewModel = remember { MapViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel() }
    val profileViewModel = remember { ProfileViewModel() }
    val profileState by profileViewModel.profileState.collectAsState()


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
                if(!title.equals("Profile")) {
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
                        ),
                    )
                }
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
                    },
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel
                )
            }
            composable("create_account") {
                CreateAccountScreen(
                    onCreateAccountSuccess = { navController.navigate("create_profile") },
                    onBackClick = { navController.navigate("login") }
                )
            }
            composable("create_profile") {
                CreateProfileScreen(
                    onCreateProfileSuccess = { navController.navigate("preferences_screen") }
                )
            }
            composable("preferences_screen") {
                val previousRoute = navController.previousBackStackEntry?.destination?.route
                val fromProfile = previousRoute == "profile"
                if (fromProfile) {
                    PreferencesScreen(
                        onBackClick = {navController.navigate("profile") },
                        onSaveClick = { navController.navigate("home") },
                        profileViewModel = profileViewModel
                    )
                } else {
                    PreferencesScreen(
                        onBackClick = {navController.navigate("create_profile") },
                        onSaveClick = { navController.navigate("select_location") },
                        profileViewModel = profileViewModel
                    )
                }
            }
            composable("select_location") {
                SelectLocationScreen(
                    viewModel = mapViewModel,
                    onLocationConfirm = { latLng ->
                        navController.navigate("home")
                    }
                )
            }
            composable("home") {
                MapScreen(
                    repository = repository,
                    // settingsViewModel = settingsViewModel,
                    onStartTalking = {
                        navController.navigate("chat")
                    }
                )
            }
            composable("chat") {
                val currentUser = authState.currentUser
                val currentUserId = currentUser?.uid ?: ""

                ChatScreen(
                    repository = repository,
                    currentUserId = currentUserId,
                    onChatClick = { roomId, userName ->
                        navController.navigate("chat_detail/$roomId/$userName")
                    },
                    onAddChatClick = {
                        navController.navigate("new_chat")
                    }
                )
            }


            composable(
                route = "chat_detail/{roomId}/{userName}",
                arguments = listOf(
                    navArgument("roomId") { type = NavType.StringType },
                    navArgument("userName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                val otherUserName = backStackEntry.arguments?.getString("userName") ?: ""

                val currentUser = authState.currentUser
                val currentUserId = currentUser?.uid ?: ""
                val currentUserName = currentUser?.displayName ?: "You"

                ChatDetailScreen(
                    userName = otherUserName,
                    roomId = roomId,
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("new_chat") {
                NewChatScreen(
                    userRepository = repository,
                    onFriendSelected = { friendId, friendName ->
                        val myUid = authState.currentUser?.uid ?: ""
                        val roomId = buildRoomId(myUid, friendId)
                        navController.navigate("chat_detail/$roomId/$friendName") {
                            popUpTo("chat") // optional, keeps back stack clean
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }


            composable("settings") {
                SettingScreen(
                    viewModel = settingsViewModel,
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel,
                    hasPhotoAccess = { true },
                    requestPhotoAccess = {},
                    onLogout = {
                        navController.navigate("login")
                               },
                    onDelete = {
                        navController.navigate("login")
                    },
                    onPrefClick = {
                        navController.navigate("preferences_screen")
                    }
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

fun buildRoomId(a: String, b: String): String {
    return if (a < b) "${a}_$b" else "${b}_$a"
}

// Temporary placeholder screens, remove functions once actual screens are implemented



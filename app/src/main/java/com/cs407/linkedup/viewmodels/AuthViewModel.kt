package com.cs407.linkedup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.linkedup.auth.*
import com.cs407.linkedup.data.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val currentUser: FirebaseUser? = null
)

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    private val _userState = MutableStateFlow(UserState())
    val userState: StateFlow<UserState> = _userState


    init {
        // Check if user is already logged in
        _authState.value = AuthState(currentUser = auth.currentUser)
    }

    fun login(email: String, password: String) {
        // Validate email
        when (checkEmail(email)) {
            EmailResult.Empty -> {
                _authState.value = _authState.value.copy(error = "Email cannot be empty")
                return
            }
            EmailResult.Invalid -> {
                _authState.value = _authState.value.copy(error = "Invalid email format")
                return
            }
            EmailResult.Valid -> { /* Continue */ }
        }

        // Validate password
        when (checkPassword(password)) {
            PasswordResult.Empty -> {
                _authState.value = _authState.value.copy(error = "Password cannot be empty")
                return
            }
            PasswordResult.Short -> {
                _authState.value = _authState.value.copy(error = "Password must be at least 5 characters")
                return
            }
            PasswordResult.Invalid -> {
                _authState.value = _authState.value.copy(
                    error = "Password must contain uppercase, lowercase, and digit"
                )
                return
            }
            PasswordResult.Valid -> { /* Continue */ }
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            when (val result = signIn(email, password, auth)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState(
                        isSuccess = true,
                        currentUser = result.user
                    )
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState(
                        error = result.message
                    )
                }
            }
        }
    }

    fun createAccount(email: String, password: String, confirmPassword: String) {
        // Validate email
        when (checkEmail(email)) {
            EmailResult.Empty -> {
                _authState.value = _authState.value.copy(error = "Email cannot be empty")
                return
            }
            EmailResult.Invalid -> {
                _authState.value = _authState.value.copy(error = "Invalid email format")
                return
            }
            EmailResult.Valid -> { /* Continue */ }
        }

        // Check if passwords match
        if (password != confirmPassword) {
            _authState.value = _authState.value.copy(error = "Passwords do not match")
            return
        }

        // Validate password
        when (checkPassword(password)) {
            PasswordResult.Empty -> {
                _authState.value = _authState.value.copy(error = "Password cannot be empty")
                return
            }
            PasswordResult.Short -> {
                _authState.value = _authState.value.copy(error = "Password must be at least 5 characters")
                return
            }
            PasswordResult.Invalid -> {
                _authState.value = _authState.value.copy(
                    error = "Password must contain uppercase, lowercase, and digit"
                )
                return
            }
            PasswordResult.Valid -> { /* Continue */ }
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            when (val result = createAccount(email, password, auth)) {
                is AuthResult.Success -> {
                    _authState.value = AuthState(
                        isSuccess = true,
                        currentUser = result.user
                    )
                    _userState.value = UserState.from(result.user)
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState(
                        error = result.message
                    )
                }
            }
        }
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank()) {
            _authState.value = _authState.value.copy(error = "Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val success = updateName(name, auth)
            if (success) {
                // Refresh current user
                val refreshed = auth.currentUser
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    currentUser = auth.currentUser
                )
                _userState.value = UserState.from(refreshed)
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Failed to update name"
                )
            }
        }
    }

    fun logout() {
        signOut(auth)
        _authState.value = AuthState()
        _userState.value = UserState()
    }

    fun resetError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun updatePreferences(interests: List<String>, classes: List<String>) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _authState.update { it.copy(error = "User not authenticated") }
            return
        }

        viewModelScope.launch {
            _authState.update { it.copy(isLoading = true) }

            val preferences = UserPreferences(interests, classes)
            val document = Firebase.firestore.collection("users").document(uid)
            val data = hashMapOf(
                "interests" to preferences.interests,
                "classes" to preferences.classes
            )
            document.set(data, SetOptions.merge()).await()
            _authState.update { it.copy(isLoading = false) }
            _userState.update { it.copy(preferences = preferences) }

        }
    }
}
package com.cs407.linkedup.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.linkedup.auth.AuthResult
import com.cs407.linkedup.auth.EmailResult
import com.cs407.linkedup.auth.PasswordResult
import com.cs407.linkedup.auth.UserState
import com.cs407.linkedup.auth.checkEmail
import com.cs407.linkedup.auth.checkPassword
import com.cs407.linkedup.auth.createAccount
import com.cs407.linkedup.auth.signIn
import com.cs407.linkedup.auth.signOut
import com.cs407.linkedup.auth.updateName
import com.cs407.linkedup.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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
                    val user = result.user
                    _authState.value = AuthState(
                        isSuccess = true,
                        currentUser = user
                    )
                    _userState.value = UserState.from(user)

                    val uid = user?.uid ?: return@launch

                    // Create a new user with only the email field initialized
                    // TODO: add contacts field once implemented
                    val userData = hashMapOf(
                        "name" to null,
                        "email" to email,
                        "phone_number" to -1,
                        "major" to null,
                        "bio" to null,
                        "location" to mapOf("lat" to -1, "lng" to -1),
                        "classes" to emptyList<String>()
                    )

                    // Add to FireStore database, using user UID as document ID
                    db.collection("users")
                        .document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d("CreateAccount", "User data added with ID: $uid")
                        }
                        .addOnFailureListener { e ->
                            Log.w("CreateAccount", "Error adding document", e)
                        }
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState(
                        error = result.message
                    )
                }
            }
        }
    }

    // TODO: Add image upload functionality
    // DO NOT upload the image to FireStore db, since it often exceeds byte limits
    fun saveProfile(name: String, major: String, bio: String, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _authState.value = _authState.value.copy(error = "Please enter a valid phone number")
            return
        }
        else if (name.isBlank()) {
            _authState.value = _authState.value.copy(error = "Please specify a name")
            return
        }
        else if (major.isBlank()) {
            _authState.value = _authState.value.copy(error = "Please specify a major, or \n'Undecided' if not applicable")
            return
        }
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val uid = auth.currentUser?.uid ?: return@launch
            db.collection("users").document(uid)
                .update(
                    "name", name,
                    "major", major,
                    "bio", bio,
                    "phone_number", phoneNumber
                )
                .addOnSuccessListener {
                    Log.d("SaveProfile", "User profile data updated with ID: $uid")
                }
                .addOnFailureListener { e ->
                    Log.w("SaveProfile", "Error updating document", e)
                }
            _authState.value = _authState.value.copy(isLoading = false, error = null)
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

    fun deleteAccount() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user == null) {
                _authState.value = _authState.value.copy(error = "No user signed in")
                return@launch
            }
            val uid = user.uid

            try {
                // Delete user from Firebase Authentication
                user.delete().await()
                _authState.value = AuthState() // Clear auth state
                _userState.value = UserState() // Clear user state

                // Delete user from Firestore Database
                db.collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener {
                        Log.d("DeleteAccount", "User data deleted with ID: $uid")
                    }
                    .addOnFailureListener { exception ->
                        Log.w("DeleteAccount", "Error deleting Firestore document", exception)
                    }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(error = e.message ?: "Failed to delete user account")
            }
        }
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

            val document = db.collection("users").document(uid)
            val data = hashMapOf(
                "interests" to preferences.interests,
                "classes" to preferences.classes
            )
            document.set(data, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.w("UpdatePreferences", "Error updating user preferences.", e)
                }
                .await()
            _authState.update { it.copy(isLoading = false) }
            _userState.update { it.copy(preferences = preferences) }

        }
    }
}
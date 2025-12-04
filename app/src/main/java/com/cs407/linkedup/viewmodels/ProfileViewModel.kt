package com.cs407.linkedup.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.linkedup.data.UserPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileState(
    val phoneNumber: String = "",
    val name: String = "",
    val major: String = "",
    val bio: String = "",
    val isLoading: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val error: String? = null,
)
class ProfileViewModel: ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _profileState = MutableStateFlow( ProfileState() )
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    init {
        viewModelScope.launch{
            val uid = auth.currentUser?.uid
            try{
                _profileState.value = _profileState.value.copy(isLoading = true)
                if (uid != null) {
                    val document = db.collection("users").document(uid).get().await()
                    if(document.exists()){
                        _profileState.value = ProfileState(
                            phoneNumber = document.getString("phone_number") ?: "",
                            name = document.getString("name") ?: "",
                            major = document.getString("major") ?: "",
                            bio = document.getString("bio") ?: "",
                            isLoading = false,
                            error = null
                        )
                    } else{
                        //Use defaults, no saved profile
                        _profileState.value = _profileState.value.copy(isLoading = false)
                    }
                } else {
                    //Use defaults, no user
                    _profileState.value = _profileState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }

    }

    fun loadProfile(uid: String?) {
        viewModelScope.launch {
            _profileState.value = ProfileState(
                phoneNumber = "",
                name = "",
                major = "",
                bio = "",
                isLoading = true,
                preferences = UserPreferences(),
                error = null
            )
            try {
                if (uid != null) {
                    val document = db.collection("users").document(uid).get().await()
                    if (document.exists()) {
                        _profileState.value = ProfileState(
                            phoneNumber = document.getString("phone_number") ?: "",
                            name = document.getString("name") ?: "",
                            major = document.getString("major") ?: "",
                            bio = document.getString("bio") ?: "",
                            preferences = UserPreferences(
                                interests = document.get("interestPrefs") as? List<String> ?: emptyList(),
                                classes = document.get("classes") as? List<String> ?: emptyList()
                                ),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _profileState.value = _profileState.value.copy(isLoading = false, error = "No info found")
                    }
                } else {
                    _profileState.value = _profileState.value.copy(isLoading = false, error = "No user signed in")
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
                _profileState.value = _profileState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateProfile(phoneNumber: String, name: String, major: String, bio: String) {
        if (phoneNumber.isBlank()) {
            _profileState.value = _profileState.value.copy(error = "Please enter a valid phone number")
            return
        }
        else if (name.isBlank()) {
            _profileState.value = _profileState.value.copy(error = "Please specify a name")
            return
        }
        else if (major.isBlank()) {
            _profileState.value = _profileState.value.copy(error = "Please specify a major, or \n'Undecided' if not applicable")
            return
        }
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true, error = null)

            try {
                val uid = auth.currentUser?.uid ?: return@launch
                Log.d("userId", uid)
                db.collection("users").document(uid)
                    .update(
                        "name", name,
                        "major", major,
                        "bio", bio,
                        "phone_number", phoneNumber
                    ).await()

                _profileState.value = _profileState.value.copy(
                    phoneNumber = phoneNumber,
                    name = name,
                    major = major,
                    bio = bio,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _profileState.value = _profileState.value.copy(isLoading = false, error = "Error updating profile")
            }
        }

    }

    fun updatePreferences(interests: List<String>, classes: List<String>) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _profileState.update { it.copy(error = "User not authenticated") }
            return
        }

        viewModelScope.launch {
            _profileState.update { it.copy(isLoading = true, error = null) }

            val preferences = UserPreferences(interests, classes)

            try {
                db.collection("users")
                    .document(uid)
                    .update(
                        mapOf(
                            "interestPrefs" to preferences.interests,
                            "classes" to preferences.classes
                        )
                    )
                    .await()

                _profileState.update {
                    it.copy(
                        isLoading = false,
                        preferences = preferences
                    )
                }
            } catch (e: Exception) {
                Log.w("UpdatePreferences", "Error updating user preferences.", e)
                _profileState.update {
                    it.copy(isLoading = false, error = "Error updating preferences")
                }
            }
        }
    }


}
package com.cs407.linkedup.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileState(
    val phoneNumber: String = "",
    val name: String = "",
    val major: String = "",
    val bio: String = "",
    val isLoading: Boolean = false,
    val error: String? = null

)
class ProfileViewModel: ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val _profileState = MutableStateFlow( ProfileState() )
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        viewModelScope.launch{
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
                _profileState.value = _profileState.value.copy(isLoading = false, error = "Error updating profile"
                )
            }
        }

    }

}
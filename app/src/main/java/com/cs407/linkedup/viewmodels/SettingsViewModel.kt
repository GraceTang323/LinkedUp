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

data class SettingsState(
    val notificationsEnabled: Boolean = true,
    val searchRadius: Float = 1.0f, // in kilometers
    val locationVisible: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SettingsViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    init {
        loadSettings()
    }

    // Load settings from Firestore
    private fun loadSettings() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _settingsState.value = _settingsState.value.copy(isLoading = true)

                val document = db.collection("users").document(uid).get().await()

                if (document.exists()) {
                    val notificationsEnabled = document.getBoolean("notifications_enabled") ?: true
                    val searchRadius = document.getDouble("search_radius")?.toFloat() ?: 1.0f
                    val locationVisible = document.getBoolean("location_visible") ?: true

                    _settingsState.value = SettingsState(
                        notificationsEnabled = notificationsEnabled,
                        searchRadius = searchRadius,
                        locationVisible = locationVisible,
                        isLoading = false
                    )
                } else {
                    // If settings don't exist, use defaults
                    _settingsState.value = _settingsState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading settings", e)
                _settingsState.value = _settingsState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Update notifications setting
    fun updateNotifications(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("notifications_enabled", enabled)
                    .await()

                _settingsState.value = _settingsState.value.copy(
                    notificationsEnabled = enabled
                )

                Log.d("SettingsViewModel", "Notifications updated: $enabled")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating notifications", e)
                _settingsState.value = _settingsState.value.copy(error = e.message)
            }
        }
    }

    // Update search radius setting
    fun updateSearchRadius(radius: Float) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("search_radius", radius.toDouble())
                    .await()

                _settingsState.value = _settingsState.value.copy(
                    searchRadius = radius
                )

                Log.d("SettingsViewModel", "Search radius updated: $radius km")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating search radius", e)
                _settingsState.value = _settingsState.value.copy(error = e.message)
            }
        }
    }

    // Update location visibility setting
    fun updateLocationVisibility(visible: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                db.collection("users").document(uid)
                    .update("location_visible", visible)
                    .await()

                _settingsState.value = _settingsState.value.copy(
                    locationVisible = visible
                )

                Log.d("SettingsViewModel", "Location visibility updated: $visible")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error updating location visibility", e)
                _settingsState.value = _settingsState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _settingsState.value = _settingsState.value.copy(error = null)
    }
}
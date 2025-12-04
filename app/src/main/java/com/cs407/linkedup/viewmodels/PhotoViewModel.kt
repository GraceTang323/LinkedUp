package com.cs407.linkedup.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Data class representing the state of photo operations
 *
 * @property photoUrl URL of the uploaded photo from Firebase Storage
 * @property isUploading Whether a photo upload is currently in progress
 * @property uploadProgress Upload progress as a percentage (0-100)
 * @property error Error message if an operation failed
 */
data class PhotoState(
    val photoUrl: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null
)

/**
 * ViewModel for managing profile photo operations
 * Handles uploading, loading, and deleting profile photos from Firebase Storage
 */
class PhotoViewModel : ViewModel() {
    // Firebase instances
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Photo state exposed to UI
    private val _photoState = MutableStateFlow(PhotoState())
    val photoState: StateFlow<PhotoState> = _photoState.asStateFlow()

    init {
        // Load existing profile photo when ViewModel is created
        loadProfilePhoto()
    }

    /**
     * Uploads a profile photo to Firebase Storage
     *
     * Process:
     * 1. Upload image file to Firebase Storage at path: profile_photos/{userId}.jpg
     * 2. Get download URL for the uploaded image
     * 3. Save the URL to Firestore user document
     *
     * @param uri Local URI of the image to upload
     */
    fun uploadProfilePhoto(uri: Uri) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _photoState.value = _photoState.value.copy(error = "User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                // Set uploading state
                _photoState.value = _photoState.value.copy(
                    isUploading = true,
                    uploadProgress = 0f,
                    error = null
                )

                // Create reference to storage location
                val storageRef = storage.reference
                val photoRef = storageRef.child("profile_photos/$userId.jpg")

                // Upload file with progress tracking
                val uploadTask = photoRef.putFile(uri)

                // Listen for upload progress
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toFloat()
                    _photoState.value = _photoState.value.copy(uploadProgress = progress)
                }.await()

                // Get download URL of uploaded image
                val downloadUrl = photoRef.downloadUrl.await()

                // Save URL to Firestore user document
                db.collection("users")
                    .document(userId)
                    .update("profile_photo_url", downloadUrl.toString())
                    .await()

                // Update state with success
                _photoState.value = PhotoState(
                    photoUrl = downloadUrl.toString(),
                    isUploading = false,
                    uploadProgress = 100f
                )

                Log.d("PhotoViewModel", "Photo uploaded successfully: $downloadUrl")

            } catch (e: Exception) {
                // Handle upload failure
                Log.e("PhotoViewModel", "Error uploading photo", e)
                _photoState.value = _photoState.value.copy(
                    isUploading = false,
                    uploadProgress = 0f,
                    error = "Failed to upload photo: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads the user's profile photo URL from Firestore
     * Called automatically when ViewModel is initialized
     */
    fun loadProfilePhoto() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Get user document from Firestore
                val document = db.collection("users").document(userId).get().await()
                val photoUrl = document.getString("profile_photo_url")

                // Update state with loaded URL
                _photoState.value = _photoState.value.copy(
                    photoUrl = photoUrl,
                    error = null
                )

                Log.d("PhotoViewModel", "Photo loaded: $photoUrl")

            } catch (e: Exception) {
                Log.e("PhotoViewModel", "Error loading photo", e)
                _photoState.value = _photoState.value.copy(
                    error = "Failed to load photo"
                )
            }
        }
    }

    /**
     * Deletes the user's profile photo
     *
     * Process:
     * 1. Delete image file from Firebase Storage
     * 2. Remove photo URL from Firestore user document
     */
    fun deleteProfilePhoto() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _photoState.value = _photoState.value.copy(isUploading = true)

                // Delete from Firebase Storage
                val storageRef = storage.reference.child("profile_photos/$userId.jpg")
                try {
                    storageRef.delete().await()
                } catch (e: Exception) {
                    // Ignore error if file doesn't exist
                    Log.w("PhotoViewModel", "Photo file not found in storage", e)
                }

                // Remove URL from Firestore
                db.collection("users")
                    .document(userId)
                    .update("profile_photo_url", null)
                    .await()

                // Update state
                _photoState.value = PhotoState(
                    photoUrl = null,
                    isUploading = false
                )

                Log.d("PhotoViewModel", "Photo deleted successfully")

            } catch (e: Exception) {
                Log.e("PhotoViewModel", "Error deleting photo", e)
                _photoState.value = _photoState.value.copy(
                    isUploading = false,
                    error = "Failed to delete photo: ${e.message}"
                )
            }
        }
    }

    /**
     * Clears any error message in the state
     */
    fun clearError() {
        _photoState.value = _photoState.value.copy(error = null)
    }
}
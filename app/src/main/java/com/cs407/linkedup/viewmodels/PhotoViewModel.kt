package com.cs407.linkedup.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

/**
 * Data class representing the state of photo operations
 *
 * @property photoBase64 Base64 encoded string of the profile photo
 * @property isUploading Whether a photo upload is currently in progress
 * @property uploadProgress Upload progress as a percentage (0-100)
 * @property error Error message if an operation failed
 */
data class PhotoState(
    val photoBase64: String? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null
)

/**
 * ViewModel for managing profile photo operations
 * Handles uploading, loading, and deleting profile photos using Base64 encoding
 * (No Firebase Storage needed - stores directly in Firestore)
 */
class PhotoViewModel : ViewModel() {
    // Firebase instances
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Photo state exposed to UI
    private val _photoState = MutableStateFlow(PhotoState())
    val photoState: StateFlow<PhotoState> = _photoState.asStateFlow()

    init {
        // Load existing profile photo when ViewModel is created
        loadProfilePhoto()
    }

    /**
     * Uploads a profile photo by converting it to Base64 and saving to Firestore
     *
     * Process:
     * 1. Read image from URI
     * 2. Resize image to reduce size (max 512x512)
     * 3. Convert to Base64 string
     * 4. Save Base64 string to Firestore user document
     *
     * @param uri Local URI of the image to upload
     * @param context Android context needed to read the image
     */
    fun uploadProfilePhoto(uri: Uri, context: Context) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _photoState.value = _photoState.value.copy(error = "User not authenticated")
            Log.e("PhotoViewModel", "Upload failed: User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                Log.d("PhotoViewModel", "Starting upload for user: $userId")

                // Set uploading state
                _photoState.value = _photoState.value.copy(
                    isUploading = true,
                    uploadProgress = 0f,
                    error = null
                )

                // Read and compress image
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap == null) {
                    throw Exception("Failed to decode image")
                }

                _photoState.value = _photoState.value.copy(uploadProgress = 25f)
                Log.d("PhotoViewModel", "Image decoded, size: ${originalBitmap.width}x${originalBitmap.height}")

                // Resize image to max 512x512 to keep size manageable
                val resizedBitmap = resizeBitmap(originalBitmap, 512, 512)
                Log.d("PhotoViewModel", "Image resized to: ${resizedBitmap.width}x${resizedBitmap.height}")

                _photoState.value = _photoState.value.copy(uploadProgress = 50f)

                // Convert to Base64
                val base64String = bitmapToBase64(resizedBitmap)
                Log.d("PhotoViewModel", "Image converted to Base64, length: ${base64String.length}")

                _photoState.value = _photoState.value.copy(uploadProgress = 75f)

                // Save Base64 string to Firestore
                db.collection("users")
                    .document(userId)
                    .set(
                        mapOf("profile_photo_base64" to base64String),
                        SetOptions.merge()
                    )
                    .await()

                Log.d("PhotoViewModel", "Base64 saved to Firestore")

                // Update state with success
                _photoState.value = PhotoState(
                    photoBase64 = base64String,
                    isUploading = false,
                    uploadProgress = 100f
                )

                Log.d("PhotoViewModel", "Photo uploaded successfully")

                // Clean up
                originalBitmap.recycle()
                resizedBitmap.recycle()

            } catch (e: Exception) {
                // Handle upload failure
                Log.e("PhotoViewModel", "Error uploading photo: ${e.javaClass.simpleName}", e)
                Log.e("PhotoViewModel", "Error message: ${e.message}")
                _photoState.value = _photoState.value.copy(
                    isUploading = false,
                    uploadProgress = 0f,
                    error = "Failed to upload photo: ${e.message}"
                )
            }
        }
    }

    /**
     * Loads the user's profile photo Base64 string from Firestore
     * Called automatically when ViewModel is initialized
     */
    fun loadProfilePhoto() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Get user document from Firestore
                val document = db.collection("users").document(userId).get().await()
                val photoBase64 = document.getString("profile_photo_base64")

                // Update state with loaded Base64
                _photoState.value = _photoState.value.copy(
                    photoBase64 = photoBase64,
                    error = null
                )

                Log.d("PhotoViewModel", "Photo loaded, has data: ${photoBase64 != null}")

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
     * 1. Remove Base64 string from Firestore user document
     */
    fun deleteProfilePhoto() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _photoState.value = _photoState.value.copy(isUploading = true)

                // Remove Base64 from Firestore
                db.collection("users")
                    .document(userId)
                    .set(
                        mapOf("profile_photo_base64" to null),
                        SetOptions.merge()
                    )
                    .await()

                // Update state
                _photoState.value = PhotoState(
                    photoBase64 = null,
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

    /**
     * Resize a bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val aspectRatio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / aspectRatio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convert bitmap to Base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress to JPEG with 80% quality to reduce size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
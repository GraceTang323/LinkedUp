package com.cs407.linkedup.repo

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val collection = firestore.collection("users")

    suspend fun saveUserLocation(location: LatLng) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("No user logged in")

        val locationMap = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude
        )

        collection.document(userId)
            .update("location", locationMap)
            .addOnFailureListener {
                e -> Log.w("UserRepository", "Error saving user location", e)
            }
            .await()
    }

    suspend fun getUserLocation(): LatLng? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = collection.document(userId).get().await()
        val map = snapshot.get("location") as? Map<*, *> ?: return null
        val lat = map["lat"] as? Double
        val lng = map["lng"] as? Double
        return if (lat != null && lng != null) LatLng(lat, lng) else null
    }
}
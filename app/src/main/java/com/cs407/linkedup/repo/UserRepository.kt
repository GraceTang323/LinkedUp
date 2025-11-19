package com.cs407.linkedup.repo

import android.util.Log
import com.cs407.linkedup.viewmodels.Student
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    // we can use callbackFlow as a channel between Firestore and the Flow
    fun getNearbyStudents(): Flow<List<Student>> = callbackFlow {
        // adding a listener allows live updates when database changes
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val students = snapshot?.documents?.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null // skips if no name
                val major = doc.getString("major") ?: ""
                val bio = doc.getString("bio") ?: ""
                val map = doc.get("location") as? Map<*, *> ?: return@mapNotNull null
                val lat = map["lat"] as? Double
                val lng = map["lng"] as? Double

                Student(
                    name = name,
                    major = major,
                    bio = bio,
                    location = LatLng(lat ?: 0.0, lng ?: 0.0)
                )
            } ?: emptyList()

            // push new list of students to flow
            trySend(students)
        }
        awaitClose { subscription.remove() } // remove Firestore listener when flow is closed
    }
}
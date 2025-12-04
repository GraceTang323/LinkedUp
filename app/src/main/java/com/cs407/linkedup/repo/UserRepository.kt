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
import com.google.firebase.firestore.FieldPath


class UserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val collection = firestore.collection("users")

    // Saves the user's chosen location to Firestore
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

    // Fetches all students in the database collection as of now, future plans include filtering by a distance cutoff
    // Uses callbackFlow as an intermediate connector between Firestore and the Flow
    fun getNearbyStudents(): Flow<List<Student>> = callbackFlow {
        // adding a listener allows live updates when database changes
        val subscription = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val students = snapshot?.documents?.mapNotNull { doc ->
                val uid = doc.id
                val name = doc.getString("name") ?: return@mapNotNull null // skips if no name
                val major = doc.getString("major") ?: ""
                val bio = doc.getString("bio") ?: ""
                val map = doc.get("location") as? Map<*, *> ?: return@mapNotNull null
                val lat = map["lat"] as? Double
                val lng = map["lng"] as? Double

                Student(
                    uid = uid,
                    name = name,
                    major = major,
                    bio = bio,
                    location = LatLng(lat ?: 0.0, lng ?: 0.0)
                )
            } ?: emptyList()

            // don't include the current user in list
            // push new list of students to flow
            trySend(students.filter({ it.uid != auth.currentUser?.uid }))
        }
        awaitClose { subscription.remove() } // remove Firestore listener when flow is closed
    }

    // Saves the current user's interest to the target user's interests subcollection
    // Updates matches for both users if a mutual interest is found
    // Returns true on a match, false otherwise
    suspend fun linkUp(targetUid: String): Boolean {
        val myUid = auth.currentUser?.uid ?: return false

        // pre-safety check to see if the other user has already matched --> prevent DUPLICATES
        val isMatchedAlready = firestore
            .collection("users")
            .document(myUid)
            .collection("matches")
            .document(targetUid)
            .get() // if the other user is new/null, get will return a missing doc by default
            .await()
            .exists()
        if (isMatchedAlready) { // exit if they're already matched
            return false
        }

        // save user's interest in the other user's interests subcollection
        firestore.collection("users")
            .document(myUid)
            .collection("interests")
            .document(targetUid)
            .set(
                mapOf("liked" to true)
            )
            .await()

        // do a check to see if the other user has already expressed interest back
        val mutual = firestore.collection("users")
            .document(targetUid)
            .collection("interests")
            .document(myUid)
            .get()
            .await()
            .exists()

        if (mutual) {
            // it's a match --> create a new match document for both users
            val match = mapOf("matched" to true)

            firestore.collection("users")
                .document(myUid)
                .collection("matches")
                .document(targetUid)
                .set(match)
                .await()
            firestore.collection("users")
                .document(targetUid)
                .collection("matches")
                .document(myUid)
                .set(match)
                .await()

            return true
        } else {
            return false // not a match yet, only one-way interest as of now
        }
    }

    suspend fun unlink(targetUid: String) {
        val myUid = auth.currentUser?.uid ?: return

        val batch = firestore.batch() // ensures all deletions are atomic --> also across one network

        val host = firestore.collection("users").document(myUid)
        val target = firestore.collection("users").document(targetUid)

        // remove target's interest and match from the host subcollections
        batch.delete(host.collection("interests").document(targetUid))
        batch.delete(host.collection("matches").document(targetUid))

        // remove user's interest and match from the target's subcollections
        batch.delete(target.collection("interests").document(myUid))
        batch.delete(target.collection("matches").document(myUid))

        try {
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Batch unlink failed", e)
            throw e
        }
    }

    // returns a list of uids of all matched users
    suspend fun getMatchedUserIds(): List<String> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = firestore
            .collection("users")
            .document(userId)
            .collection("matches")
            .get()
            .await()
        return snapshot.documents.map { it.id }
    }

    data class MatchedUser(
        val uid: String,
        val name: String
    )

    suspend fun getMatchedUsers(): List<MatchedUser> {
        val myUid = auth.currentUser?.uid ?: return emptyList()

        val matchesSnapshot = firestore
            .collection("users")
            .document(myUid)
            .collection("matches")
            .get()
            .await()

        val matchedIds = matchesSnapshot.documents.map { it.id }
        if (matchedIds.isEmpty()) return emptyList()

        val users = mutableListOf<MatchedUser>()
        for (id in matchedIds) {
            val doc = firestore.collection("users").document(id).get().await()
            val name = doc.getString("name") ?: continue
            users.add(MatchedUser(uid = id, name = name))
        }

        return users
    }


    // removes the deleted user's id from others' interests and matches subcollections in firebase
    // cleans up database when a user is deleted
    suspend fun cascade_delete(userId: String) {
        val usersRef = firestore.collection("users")

        val usersQuery = usersRef.get().await()

        for (doc in usersQuery.documents) {
            val uid = doc.id
            if (uid == userId) continue // skip deleted user's uid, removed later in authViewModel

            // delete user from other user's matches
            usersRef.document(uid)
                .collection("matches")
                .document(userId)
                .delete()
                .await()
            // delete user from other user's interests
            usersRef.document(uid)
                .collection("interests")
                .document(userId)
                .delete()
                .await()
        }

        // delete the user's subcollections --> interests, matches
        val userInterests = usersRef
            .document(userId)
            .collection("interests")
            .get()
            .await()
        for (doc in userInterests.documents) {
            doc.reference.delete().await()
        }

        val userMatches = usersRef
            .document(userId)
            .collection("matches")
            .get()
            .await()
        for (doc in userMatches.documents) {
            doc.reference.delete().await()
        }
    }
}
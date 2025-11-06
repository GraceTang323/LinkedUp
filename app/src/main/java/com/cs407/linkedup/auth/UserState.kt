package com.cs407.linkedup.auth

import com.cs407.linkedup.data.UserPreferences
import com.google.firebase.auth.FirebaseUser

/**
 * UserState
 * Represents the current authentication state of the user
 */
data class UserState(
    val user: FirebaseUser? = null,
    val isAuthenticated: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val uid: String? = null,
    val preferences: UserPreferences = UserPreferences()
) {
    companion object {
        fun from(firebaseUser: FirebaseUser?): UserState {
            return if (firebaseUser != null) {
                UserState(
                    user = firebaseUser,
                    isAuthenticated = true,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    uid = firebaseUser.uid
                )
            } else {
                UserState()
            }
        }
    }
}
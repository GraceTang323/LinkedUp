package com.cs407.linkedup.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Authentication Helper
 * Contains all business logic for Firebase authentication
 */

// ============================================
// Email Validation
// ============================================

enum class EmailResult {
    Valid,
    Empty,
    Invalid,
}

fun checkEmail(email: String): EmailResult {
    if (email.isEmpty()) {
        return EmailResult.Empty
    }

    // 1. username of email should only contain "0-9, a-z, _, A-Z, ."
    // 2. there is one and only one "@" between username and server address
    // 3. there are multiple domain names with at least one top-level domain
    // 4. domain name "0-9, a-z, -, A-Z" (could not have "_" but "-" is valid)
    // 5. multiple domain separate with '.'
    // 6. top level domain should only contain letters and at least 2 letters
    val pattern = Regex("^[\\w.]+@([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$")

    return if (email.matches(pattern)) {
        EmailResult.Valid
    } else {
        EmailResult.Invalid
    }
}

// ============================================
// Password Validation
// ============================================

enum class PasswordResult {
    Valid,
    Empty,
    Short,
    Invalid
}

fun checkPassword(password: String): PasswordResult {
    // 1. password should contain at least one uppercase letter, lowercase letter, one digit
    // 2. minimum length: 5
    if (password.isEmpty()) {
        return PasswordResult.Empty
    }

    if (password.length < 5) {
        return PasswordResult.Short
    }

    if (Regex("\\d+").containsMatchIn(password) &&
        Regex("[a-z]+").containsMatchIn(password) &&
        Regex("[A-Z]+").containsMatchIn(password)
    ) {
        return PasswordResult.Valid
    }

    return PasswordResult.Invalid
}

// ============================================
// Firebase Authentication Functions
// ============================================

/**
 * Sign in existing user with email and password
 */
suspend fun signIn(
    email: String,
    password: String,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
): AuthResult {
    return try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        AuthResult.Success(result.user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Sign in failed")
    }
}

/**
 * Create new Firebase account with email and password
 */
suspend fun createAccount(
    email: String,
    password: String,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
): AuthResult {
    return try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        AuthResult.Success(result.user)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Account creation failed")
    }
}

/**
 * Update Firebase Auth displayName
 * Used for username collection
 */
suspend fun updateName(
    name: String,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
): Boolean {
    return try {
        val user = auth.currentUser ?: return false
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Sign out current user
 */
fun signOut(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    auth.signOut()
}

// ============================================
// Result Classes
// ============================================

sealed class AuthResult {
    data class Success(val user: com.google.firebase.auth.FirebaseUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
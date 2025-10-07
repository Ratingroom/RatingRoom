package com.example.ratingroom.data.datasource

import com.google.firebase.auth.FirebaseUser

interface AuthRemoteDataSource {
    val currentUser: FirebaseUser?
    suspend fun signIn(email: String, password: String): FirebaseUser?
    suspend fun signUp(email: String, password: String, displayName: String?): FirebaseUser?
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun updateUserEmail(newEmail: String)
    suspend fun updateDisplayName(displayName: String)
    fun signOut()
    fun isUserLoggedIn(): Boolean
}
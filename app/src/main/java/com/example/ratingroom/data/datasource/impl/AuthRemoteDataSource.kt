package com.example.ratingroom.data.datasource.impl

import com.example.ratingroom.data.datasource.AuthRemoteDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authService: FirebaseAuth
) : AuthRemoteDataSource {


    override val currentUser: FirebaseUser? get() = authService.currentUser

    override suspend fun signIn(email: String, password: String): FirebaseUser? {
        val result = authService.signInWithEmailAndPassword(email, password).await()
        return result.user
    }

    override suspend fun signUp(
        email: String, 
        password: String, 
        displayName: String?
    ): FirebaseUser? {
        println("AuthRemoteDataSource: Creando usuario con Firebase Auth")
        val result = authService.createUserWithEmailAndPassword(email, password).await()
        val user = result.user
        
        // Actualizar el displayName si se proporciona
        displayName?.let {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(it)
                .build()
            user?.updateProfile(profileUpdates)?.await()
        }
        
        println("AuthRemoteDataSource: Usuario creado exitosamente con UID: ${user?.uid}")
        return user
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        authService.sendPasswordResetEmail(email).await()
    }

    override suspend fun updateUserEmail(newEmail: String) {
        val user = currentUser ?: throw IllegalStateException("Usuario no autenticado")
        if (newEmail != user.email) {
            println("AuthRemoteDataSource.updateUserEmail: Actualizando email en Firebase Auth: $newEmail")
            user.updateEmail(newEmail).await()
            println("AuthRemoteDataSource.updateUserEmail: Email actualizado en Firebase Auth")
        }
    }
    
    override suspend fun updateDisplayName(displayName: String) {
        val user = currentUser ?: throw IllegalStateException("Usuario no autenticado")
        println("AuthRemoteDataSource.updateDisplayName: Actualizando displayName en Firebase Auth: $displayName")
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        user.updateProfile(profileUpdates).await()
        println("AuthRemoteDataSource.updateDisplayName: displayName actualizado en Firebase Auth")
    }

    override fun signOut() {
        authService.signOut()
    }

    override fun isUserLoggedIn(): Boolean {
        return currentUser != null
    }
}
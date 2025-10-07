package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.AuthRemoteDataSource
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

data class UserProfile(
    val uid: String,
    val email: String,
    val fullName: String? = null,
    val favoriteGenre: String? = null,
    val birthYear: String? = null,
    val biography: String? = null,
    val location: String? = null,
    val birthdate: String? = null,
    val website: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

@Singleton
class AuthRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val firestoreDataSource: FirestoreDataSource
) {

    val currentUser: FirebaseUser? get() = authRemoteDataSource.currentUser

    // ---------- Sign In ----------
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return runCatching {
            authRemoteDataSource.signIn(email, password)
                ?: error("No se pudo iniciar sesión.")
        }.mapErrorAuth()
    }

    // ---------- Sign Up ----------
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return runCatching {
            val user = authRemoteDataSource.signUp(
                email = email,
                password = password,
                displayName = displayName
            )
            user ?: error("No se pudo crear la cuenta.")
        }.mapErrorAuth()
    }

    // ---------- Password Reset ----------
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return runCatching {
            authRemoteDataSource.sendPasswordResetEmail(email)
        }.mapErrorAuth()
    }

    // ---------- Update Profile (Auth + Firestore) ----------
    suspend fun updateUserProfile(
        displayName: String? = null,
        email: String? = null,
        biography: String? = null,
        location: String? = null,
        favoriteGenre: String? = null,
        birthdate: String? = null,
        website: String? = null,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return runCatching {
            // Cambios en Firebase Auth si aplica
            displayName?.let { authRemoteDataSource.updateDisplayName(it) }
            email?.let { authRemoteDataSource.updateUserEmail(it) }

            // Cambios en Firestore
            firestoreDataSource.updateUserProfile(
                displayName = displayName,
                email = email,
                biography = biography,
                location = location,
                favoriteGenre = favoriteGenre,
                birthdate = birthdate,
                website = website,
                profileImageUrl = profileImageUrl
            )
        }.mapErrorAuth()
    }

    // ---------- Perfil ----------
    suspend fun getUserProfile(): Result<UserProfile> {
        return runCatching {
            val user = currentUser ?: error("No hay usuario autenticado.")
            val profileData = firestoreDataSource.getUserProfile()

            if (profileData != null) {
                val profileImageUrl = profileData["profileImageUrl"] as? String
                UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    fullName = profileData["fullName"] as? String,
                    favoriteGenre = profileData["favoriteGenre"] as? String,
                    birthYear = profileData["birthYear"] as? String,
                    biography = profileData["biography"] as? String,
                    location = profileData["location"] as? String,
                    birthdate = profileData["birthdate"] as? String,
                    website = profileData["website"] as? String,
                    profileImageUrl = profileImageUrl,
                    createdAt = profileData["createdAt"] as? Long,
                    updatedAt = profileData["updatedAt"] as? Long
                )
            } else {
                UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    fullName = user.displayName
                )
            }
        }.mapErrorAuth()
    }

    // ---------- Utilidades ----------
    fun signOut() {
        authRemoteDataSource.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return authRemoteDataSource.isUserLoggedIn()
    }
}

/** Mapea excepciones de Auth/Network a mensajes específicos dentro de Result */
private fun <T> Result<T>.mapErrorAuth(): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { t ->
            when (t) {
                is CancellationException -> throw t
                is FirebaseAuthInvalidCredentialsException ->
                    Result.failure(IllegalStateException("Credenciales inválidas. Verifica tu email o contraseña."))
                is FirebaseAuthInvalidUserException ->
                    Result.failure(IllegalStateException("El usuario no existe o fue deshabilitado."))
                is FirebaseAuthUserCollisionException ->
                    Result.failure(IllegalStateException("El email ya está en uso."))
                is FirebaseAuthWeakPasswordException ->
                    Result.failure(IllegalStateException("La contraseña es demasiado débil."))
                is FirebaseAuthRecentLoginRequiredException ->
                    Result.failure(IllegalStateException("Por seguridad, vuelve a iniciar sesión para continuar."))
                is FirebaseAuthActionCodeException ->
                    Result.failure(IllegalStateException("Código inválido o expirado."))
                is FirebaseNetworkException ->
                    Result.failure(IllegalStateException("Sin conexión. Intenta de nuevo."))
                else ->
                    Result.failure(IllegalStateException(t.message ?: "Error inesperado."))
            }
        }
    )

}

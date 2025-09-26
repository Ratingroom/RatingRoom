package com.example.ratingroom.data.repository

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

data class AuthResult(
    val isSuccess: Boolean,
    val user: FirebaseUser? = null,
    val errorMessage: String? = null
)

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

    // -------------------------
    // Sign In
    // -------------------------
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val user = authRemoteDataSource.signIn(email, password)
            AuthResult(
                isSuccess = user != null,
                user = user
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AuthResult(isSuccess = false, errorMessage = "Credenciales inválidas. Verifica tu email o contraseña.")
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult(isSuccess = false, errorMessage = "El usuario no existe o fue deshabilitado.")
        } catch (e: FirebaseNetworkException) {
            AuthResult(isSuccess = false, errorMessage = "Sin conexión. Intenta de nuevo.")
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Error de autenticación")
        }
    }

    // -------------------------
    // Sign Up
    // -------------------------
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResult {
        return try {
            println("AuthRepository: Iniciando registro para email: $email")
            val user = authRemoteDataSource.signUp(
                email = email,
                password = password,
                displayName = displayName
            )
            println("AuthRepository: Usuario registrado: ${user?.uid}")
            AuthResult(
                isSuccess = user != null,
                user = user
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthUserCollisionException) {
            println("AuthRepository: Email en uso: ${e.message}")
            AuthResult(isSuccess = false, errorMessage = "El email ya está en uso.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            println("AuthRepository: Contraseña débil: ${e.message}")
            AuthResult(isSuccess = false, errorMessage = "La contraseña es demasiado débil.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            println("AuthRepository: Credenciales inválidas: ${e.message}")
            AuthResult(isSuccess = false, errorMessage = "Email inválido. Revisa el formato.")
        } catch (e: FirebaseNetworkException) {
            println("AuthRepository: Error de red: ${e.message}")
            AuthResult(isSuccess = false, errorMessage = "Sin conexión. Intenta de nuevo.")
        } catch (e: Exception) {
            println("AuthRepository: Error en registro: ${e.message}")
            e.printStackTrace()
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Error al crear cuenta")
        }
    }

    // -------------------------
    // Password Reset
    // -------------------------
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            authRemoteDataSource.sendPasswordResetEmail(email)
            AuthResult(isSuccess = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            AuthResult(isSuccess = false, errorMessage = "No existe una cuenta con ese email.")
        } catch (e: FirebaseAuthActionCodeException) {
            AuthResult(isSuccess = false, errorMessage = "No se pudo procesar la solicitud de recuperación.")
        } catch (e: FirebaseNetworkException) {
            AuthResult(isSuccess = false, errorMessage = "Sin conexión. Intenta de nuevo.")
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Error al enviar email de recuperación")
        }
    }

    // -------------------------
    // Update Profile (Auth + Firestore)
    // -------------------------
    suspend fun updateUserProfile(
        displayName: String? = null,
        email: String? = null,
        biography: String? = null,
        location: String? = null,
        favoriteGenre: String? = null,
        birthdate: String? = null,
        website: String? = null,
        profileImageUrl: String? = null
    ): AuthResult {
        return try {
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
            AuthResult(isSuccess = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            AuthResult(isSuccess = false, errorMessage = "Por seguridad, vuelve a iniciar sesión para continuar.")
        } catch (e: FirebaseAuthUserCollisionException) {
            // Cambiar email a uno ya registrado
            AuthResult(isSuccess = false, errorMessage = "El email nuevo ya está en uso.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            // Email con formato inválido u otro problema de credenciales
            AuthResult(isSuccess = false, errorMessage = "Email inválido. Revisa el formato.")
        } catch (e: FirebaseNetworkException) {
            AuthResult(isSuccess = false, errorMessage = "Sin conexión. Intenta de nuevo.")
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.message ?: "Error al actualizar perfil")
        }
    }

    // -------------------------
    // Perfil
    // -------------------------
    suspend fun getUserProfile(): UserProfile? {
        return try {
            println("AuthRepository.getUserProfile: Iniciando obtención de perfil")
            val user = currentUser ?: return null
            println("AuthRepository.getUserProfile: Usuario autenticado: ${user.uid}")

            val profileData = firestoreDataSource.getUserProfile()
            println("AuthRepository.getUserProfile: Datos recibidos de FirestoreDataSource: ${profileData != null}")

            if (profileData != null) {
                val profileImageUrl = profileData["profileImageUrl"] as? String
                println("AuthRepository.getUserProfile: profileImageUrl recuperado: $profileImageUrl")

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
                ).also {
                    println("AuthRepository.getUserProfile: Perfil creado con profileImageUrl: ${it.profileImageUrl}")
                }
            } else {
                // Si no hay datos en Firestore, crear perfil básico
                println("AuthRepository.getUserProfile: No hay datos en Firestore, creando perfil básico")
                UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    fullName = user.displayName
                )
            }
        } catch (e: Exception) {
            println("AuthRepository.getUserProfile: ERROR al obtener perfil: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun signOut() {
        authRemoteDataSource.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return authRemoteDataSource.isUserLoggedIn()
    }
}
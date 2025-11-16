package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.AuthRemoteDataSource
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.utils.FCMTokenManager
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
    val mainMovieId: Int? = null, // 🎬 Película principal del usuario
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val followersCount: Int? = 0,
    val followingCount: Int? = 0
)

@Singleton
class AuthRepository @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val fcmTokenManager: FCMTokenManager
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
        displayName: String? = null,
        favoriteGenre: String? = null,
        birthYear: String? = null
    ): Result<FirebaseUser> {
        return runCatching {
            // 1) Crear usuario en Auth
            val user = authRemoteDataSource.signUp(
                email = email,
                password = password,
                displayName = displayName
            ) ?: error("No se pudo crear la cuenta.")

            // 2) Crear documento en Firestore
            firestoreDataSource.createUserDocument(
                userId = user.uid,
                email = email,
                fullName = displayName,
                favoriteGenre = favoriteGenre,
                birthYear = birthYear
            )

            user
        }.mapErrorAuth()
    }

    // ---------- Sign In with Google ----------
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return runCatching {
            val user = authRemoteDataSource.signInWithGoogle(idToken)
                ?: error("No se pudo iniciar sesión con Google.")
            
            // Verificar si el usuario ya existe en Firestore, si no, crear documento
            val existingProfile = firestoreDataSource.getUserProfileById(user.uid)
            if (existingProfile == null) {
                firestoreDataSource.createUserDocument(
                    userId = user.uid,
                    email = user.email ?: "",
                    fullName = user.displayName,
                    favoriteGenre = null,
                    birthYear = null
                )
            }
            
            user
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
        profileImageUrl: String? = null,
        mainMovieId: Int? = null
    ): Result<Unit> {
        return runCatching {
            // Cambios en Firebase Auth
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
                profileImageUrl = profileImageUrl,
                mainMovieId = mainMovieId
            )
        }.mapErrorAuth()
    }

    // ---------- Perfil: "Mi perfil" ----------
    suspend fun getUserProfile(): Result<UserProfile> {
        return runCatching {
            val user = currentUser ?: error("No hay usuario autenticado.")
            val profileData = firestoreDataSource.getUserProfile()

            if (profileData != null) {
                val profileImageUrl = profileData["profileImageUrl"] as? String
                val mainMovieId = (profileData["mainMovieId"] as? Number)?.toInt()
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
                    mainMovieId = mainMovieId,
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

    // ---------- Perfil: "Ver otro usuario" por ID ----------
    suspend fun getUserProfileById(userId: String): Result<UserProfile> {
        return runCatching {
            val profileData = firestoreDataSource.getUserProfileById(userId)
                ?: error("Usuario no encontrado.")

            val email = profileData["email"] as? String ?: ""
            val profileImageUrl = profileData["profileImageUrl"] as? String
            val mainMovieId = (profileData["mainMovieId"] as? Number)?.toInt()

            UserProfile(
                uid = userId,
                email = email,
                fullName = profileData["fullName"] as? String,
                favoriteGenre = profileData["favoriteGenre"] as? String,
                birthYear = profileData["birthYear"] as? String,
                biography = profileData["biography"] as? String,
                location = profileData["location"] as? String,
                birthdate = profileData["birthdate"] as? String,
                website = profileData["website"] as? String,
                profileImageUrl = profileImageUrl,
                mainMovieId = mainMovieId,
                createdAt = profileData["createdAt"] as? Long,
                updatedAt = profileData["updatedAt"] as? Long
            )
        }.mapErrorAuth()
    }

    // ---------- Seguidores y Seguidos ----------
    suspend fun followUser(targetUserId: String): Result<Boolean> {
        return runCatching {
            firestoreDataSource.followUser(targetUserId)
        }.mapErrorAuth()
    }
    
    suspend fun unfollowUser(targetUserId: String): Result<Boolean> {
        return runCatching {
            firestoreDataSource.unfollowUser(targetUserId)
        }.mapErrorAuth()
    }
    
    suspend fun getFollowers(userId: String = ""): Result<List<UserProfile>> {
        return runCatching {
            val uid = userId.ifEmpty { currentUser?.uid ?: error("No hay usuario autenticado.") }
            val followers = firestoreDataSource.getFollowers(uid)
            
            followers.map { profileData ->
                val uid = profileData["uid"] as? String ?: ""
                val email = profileData["email"] as? String ?: ""
                val profileImageUrl = profileData["profileImageUrl"] as? String
                
                UserProfile(
                    uid = uid,
                    email = email,
                    fullName = profileData["fullName"] as? String,
                    favoriteGenre = profileData["favoriteGenre"] as? String,
                    biography = profileData["biography"] as? String,
                    location = profileData["location"] as? String,
                    profileImageUrl = profileImageUrl,
                    followersCount = (profileData["followersCount"] as? Number)?.toInt(),
                    followingCount = (profileData["followingCount"] as? Number)?.toInt()
                )
            }
        }.mapErrorAuth()
    }
    
    suspend fun getFollowing(userId: String = ""): Result<List<UserProfile>> {
        return runCatching {
            val uid = userId.ifEmpty { currentUser?.uid ?: error("No hay usuario autenticado.") }
            val following = firestoreDataSource.getFollowing(uid)
            
            following.map { profileData ->
                val uid = profileData["uid"] as? String ?: ""
                val email = profileData["email"] as? String ?: ""
                val profileImageUrl = profileData["profileImageUrl"] as? String
                
                UserProfile(
                    uid = uid,
                    email = email,
                    fullName = profileData["fullName"] as? String,
                    favoriteGenre = profileData["favoriteGenre"] as? String,
                    biography = profileData["biography"] as? String,
                    location = profileData["location"] as? String,
                    profileImageUrl = profileImageUrl,
                    followersCount = (profileData["followersCount"] as? Number)?.toInt(),
                    followingCount = (profileData["followingCount"] as? Number)?.toInt()
                )
            }
        }.mapErrorAuth()
    }

    // ---------- Utilidades ----------
    suspend fun signOut(context: android.content.Context? = null) {
        // Limpiar token FCM antes de cerrar sesión
        fcmTokenManager.clearFCMToken()
        
        // Limpiar credenciales de Google del Credential Manager
        context?.let {
            authRemoteDataSource.clearGoogleCredentials(it)
        }
        
        // Cerrar sesión en Firebase Auth
        authRemoteDataSource.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return authRemoteDataSource.isUserLoggedIn()
    }
}

/** Mapeo de errores comunes de Firebase a mensajes legibles */
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

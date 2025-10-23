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

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val user = authRemoteDataSource.signIn(email, password)
                ?: throw IllegalStateException("No se pudo iniciar sesión.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val user = authRemoteDataSource.signInWithGoogle(idToken)
                ?: throw IllegalStateException("No se pudo iniciar sesión con Google.")
            
            // Verificar si es la primera vez que se registra (crear documento en Firestore)
            val userExists = firestoreDataSource.getUserProfileById(user.uid) != null
            if (!userExists) {
                firestoreDataSource.createUserDocument(
                    userId = user.uid,
                    email = user.email ?: "",
                    fullName = user.displayName,
                    favoriteGenre = null,
                    birthYear = null
                )
            }
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null,
        favoriteGenre: String? = null,
        birthYear: String? = null
    ): Result<FirebaseUser> {
        return try {
            val user = authRemoteDataSource.signUp(
                email = email,
                password = password,
                displayName = displayName
            ) ?: throw IllegalStateException("No se pudo crear la cuenta.")

            firestoreDataSource.createUserDocument(
                userId = user.uid,
                email = email,
                fullName = displayName,
                favoriteGenre = favoriteGenre,
                birthYear = birthYear
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun sendPasswordResetEmail(email: String) {
        authRemoteDataSource.sendPasswordResetEmail(email)
    }

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
    ) {
        displayName?.let { authRemoteDataSource.updateDisplayName(it) }
        email?.let { authRemoteDataSource.updateUserEmail(it) }

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
    }

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val user = currentUser ?: throw IllegalStateException("No hay usuario autenticado.")
            val profileData = firestoreDataSource.getUserProfileById(user.uid)

            val profile = if (profileData != null) {
                UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    fullName = profileData.fullName,
                    favoriteGenre = profileData.favoriteGenre,
                    birthYear = profileData.birthYear,
                    biography = profileData.biography,
                    location = profileData.location,
                    birthdate = profileData.birthdate,
                    website = profileData.website,
                    profileImageUrl = profileData.profileImageUrl,
                    mainMovieId = profileData.mainMovieId,
                    createdAt = profileData.createdAt,
                    updatedAt = profileData.updatedAt,
                    followersCount = profileData.followersCount,
                    followingCount = profileData.followingCount
                )
            } else {
                UserProfile(
                    uid = user.uid,
                    email = user.email ?: "",
                    fullName = user.displayName,
                    followersCount = 0,
                    followingCount = 0
                )
            }
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun getUserProfileById(userId: String): Result<UserProfile> {
        return try {
            val profileData = firestoreDataSource.getUserProfileById(userId)
                ?: throw IllegalStateException("Usuario no encontrado.")

            val profile = UserProfile(
                uid = userId,
                email = profileData.email ?: "",
                fullName = profileData.fullName,
                favoriteGenre = profileData.favoriteGenre,
                birthYear = profileData.birthYear,
                biography = profileData.biography,
                location = profileData.location,
                birthdate = profileData.birthdate,
                website = profileData.website,
                profileImageUrl = profileData.profileImageUrl,
                mainMovieId = profileData.mainMovieId,
                createdAt = profileData.createdAt,
                updatedAt = profileData.updatedAt
            )
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun followUser(targetUserId: String): Boolean {
        return firestoreDataSource.followUser(targetUserId)
    }
    
    suspend fun unfollowUser(targetUserId: String): Boolean {
        return firestoreDataSource.unfollowUser(targetUserId)
    }
    
    suspend fun getFollowers(userId: String = ""): Result<List<UserProfile>> {
        return try {
            val uid = userId.ifEmpty { currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado.") }
            val followers = firestoreDataSource.getFollowers(uid)
            
            val profiles = followers.map { profileData ->
                UserProfile(
                    uid = profileData.uid,
                    email = profileData.email ?: "",
                    fullName = profileData.fullName,
                    favoriteGenre = profileData.favoriteGenre,
                    biography = profileData.biography,
                    location = profileData.location,
                    profileImageUrl = profileData.profileImageUrl,
                    followersCount = profileData.followersCount,
                    followingCount = profileData.followingCount
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }
    
    suspend fun getFollowing(userId: String = ""): Result<List<UserProfile>> {
        return try {
            val uid = userId.ifEmpty { currentUser?.uid ?: throw IllegalStateException("No hay usuario autenticado.") }
            val following = firestoreDataSource.getFollowing(uid)
            
            val profiles = following.map { profileData ->
                UserProfile(
                    uid = profileData.uid,
                    email = profileData.email ?: "",
                    fullName = profileData.fullName,
                    favoriteGenre = profileData.favoriteGenre,
                    biography = profileData.biography,
                    location = profileData.location,
                    profileImageUrl = profileData.profileImageUrl,
                    followersCount = profileData.followersCount,
                    followingCount = profileData.followingCount
                )
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }.mapErrorAuth()
    }

    suspend fun signOut() {
        fcmTokenManager.clearFCMToken()
        authRemoteDataSource.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return authRemoteDataSource.isUserLoggedIn()
    }
}

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

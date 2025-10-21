package com.example.ratingroom.data.datasource.impl

import android.util.Log
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.MovieDto
import com.example.ratingroom.data.dtos.NotificationDto
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.data.dtos.UserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreDataSourceImpl @Inject constructor(
    private val firestoreService: FirebaseFirestore,
    private val authService: FirebaseAuth
) : FirestoreDataSource {

    private val currentUserId: String?
        get() = authService.currentUser?.uid

    // ---------------- MAPPERS ----------------
    private fun DocumentSnapshot.toUserDto(): UserDto? {
        if (!exists()) return null
        val data = data ?: return null
        
        return UserDto(
            uid = id,
            id = 0,
            displayName = (data["displayName"] as? String) ?: (data["fullName"] as? String) ?: "",
            fullName = (data["fullName"] as? String) ?: (data["displayName"] as? String) ?: "",
            email = (data["email"] as? String) ?: "",
            username = (data["username"] as? String) ?: (data["email"] as? String)?.substringBefore("@") ?: "",
            biography = (data["biography"] as? String) ?: "",
            location = (data["location"] as? String) ?: "",
            favoriteGenre = (data["favoriteGenre"] as? String) ?: "",
            birthYear = (data["birthYear"] as? String) ?: "",
            birthdate = (data["birthdate"] as? String) ?: "",
            website = (data["website"] as? String) ?: "",
            profileImageUrl = data["profileImageUrl"] as? String,
            mainMovieId = (data["mainMovieId"] as? Number)?.toInt(),
            followersCount = (data["followersCount"] as? Number)?.toInt(),
            followingCount = (data["followingCount"] as? Number)?.toInt(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun DocumentSnapshot.toReviewDto(): ReviewDto? {
        if (!exists()) return null
        val data = data ?: return null
        
        return ReviewDto(
            id = (data["id"] as? String) ?: id,
            rating = (data["rating"] as? Number)?.toInt() ?: 0,
            texto = (data["text"] as? String) ?: "",
            usuario_id = 0,
            pelicula_id = (data["movieId"] as? Number)?.toInt() ?: 0,
            userName = data["userName"] as? String,
            userImageUrl = data["userImageUrl"] as? String,
            likes = (data["likes"] as? Number)?.toInt() ?: 0
        )
    }

    private fun DocumentSnapshot.toMovieDto(): MovieDto? {
        if (!exists()) return null
        val data = data ?: return null
        
        return MovieDto(
            id = (data["id"] as? Number)?.toInt() ?: id.toIntOrNull() ?: 0,
            title = (data["title"] as? String) ?: "",
            titulo = (data["titulo"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            descripcion = (data["descripcion"] as? String) ?: "",
            year = (data["year"] as? String) ?: "",
            fechaSalida = (data["releaseDate"] as? String) ?: (data["fechaSalida"] as? String) ?: "",
            imageUrl = (data["imageUrl"] as? String) ?: (data["poster"] as? String),
            portada = (data["portada"] as? String),
            genre = (data["genre"] as? String) ?: "",
            rating = (data["rating"] as? Number)?.toDouble() ?: 0.0,
            reviews = (data["reviews"] as? Number)?.toInt() ?: 0,
            director = (data["director"] as? String) ?: "",
            duration = (data["duration"] as? String) ?: ""
        )
    }

    private fun DocumentSnapshot.toNotificationDto(): NotificationDto? {
        if (!exists()) return null
        val data = data ?: return null
        
        return NotificationDto(
            id = (data["id"] as? String) ?: id,
            type = (data["type"] as? String) ?: "",
            actorUserId = (data["actorUserId"] as? String) ?: "",
            actorName = data["actorName"] as? String,
            reviewId = data["reviewId"] as? String,
            movieId = (data["movieId"] as? Number)?.toInt(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            seen = (data["seen"] as? Boolean) ?: false
        )
    }

    // ---------------- PERFIL ----------------
    override suspend fun createUserDocument(
        userId: String,
        email: String,
        fullName: String?,
        favoriteGenre: String?,
        birthYear: String?
    ) {
        val data = mutableMapOf<String, Any>(
            "email" to email,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        fullName?.let { data["fullName"] = it }
        favoriteGenre?.let { data["favoriteGenre"] = it }
        birthYear?.let { data["birthYear"] = it }

        firestoreService.collection("users").document(userId).set(data).await()
    }

    override suspend fun updateUserProfile(
        displayName: String?,
        email: String?,
        biography: String?,
        location: String?,
        favoriteGenre: String?,
        birthdate: String?,
        website: String?,
        profileImageUrl: String?,
        mainMovieId: Int?
    ) {
        val userId = currentUserId ?: throw IllegalStateException("Usuario no autenticado")

        val updates = mutableMapOf<String, Any>()
        displayName?.let { updates["fullName"] = it }
        email?.let { updates["email"] = it }
        biography?.let { updates["biography"] = it }
        location?.let { updates["location"] = it }
        favoriteGenre?.let { updates["favoriteGenre"] = it }
        birthdate?.let { updates["birthdate"] = it }
        website?.let { updates["website"] = it }
        profileImageUrl?.let { updates["profileImageUrl"] = it }
        mainMovieId?.let { updates["mainMovieId"] = it }
        updates["updatedAt"] = System.currentTimeMillis()

        val docRef = firestoreService.collection("users").document(userId)
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            docRef.update(updates).await()
        } else {
            updates["createdAt"] = System.currentTimeMillis()
            docRef.set(updates).await()
        }
    }

    override suspend fun getUserProfile(): UserDto? {
        val userId = currentUserId ?: return null
        val doc = firestoreService.collection("users").document(userId).get().await()
        return doc.toUserDto()
    }

    override suspend fun getUserProfileById(userId: String): UserDto? {
        val doc = firestoreService.collection("users").document(userId).get().await()
        return doc.toUserDto()
    }

    override suspend fun getAllUsers(): List<UserDto> {
        val snap = firestoreService.collection("users").get().await()
        return snap.documents.mapNotNull { it.toUserDto() }
    }

    override suspend fun createReviewFanout(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): String {
        val userProfile = getUserProfileById(userId)
        val userName = userProfile?.fullName ?: "Usuario"
        val userImageUrl = userProfile?.profileImageUrl

        val reviewDoc = firestoreService.collection("reviews").document()
        val now = System.currentTimeMillis()

        val reviewData = mapOf(
            "id" to reviewDoc.id,
            "userId" to userId,
            "movieId" to movieId,
            "rating" to rating,
            "text" to text,
            "likes" to 0,
            "createdAt" to now,
            "updatedAt" to now,
            "userName" to userName,
            "userImageUrl" to (userImageUrl ?: "")
        )

        reviewDoc.set(reviewData).await()

        return reviewDoc.id
    }

    override suspend fun getReviewsByMovie(movieId: Int): List<ReviewDto> {
        val snapshot = firestoreService.collection("reviews")
            .whereEqualTo("movieId", movieId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toReviewDto() }
            .sortedByDescending { it.id } // Ordenar en código
    }

    override suspend fun getReviewsByUser(userId: String): List<ReviewDto> {
        val snapshot = firestoreService.collection("reviews")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.toReviewDto() }
            .sortedByDescending { it.id } // Ordenar en código
    }

    override fun observeReviewsByMovie(movieId: Int): Flow<List<ReviewDto>> = callbackFlow {
        val ref = firestoreService.collection("reviews")
            .whereEqualTo("movieId", movieId)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("Firestore", "Error observando reseñas: ${err.message}")
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { it.toReviewDto() }
                    .sortedByDescending { it.id } // Ordenar por ID en código (más reciente primero)
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeReviewsByUser(userId: String): Flow<List<ReviewDto>> = callbackFlow {
        val ref = firestoreService.collection("reviews")
            .whereEqualTo("userId", userId)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("Firestore", "Error observando reseñas de usuario: ${err.message}")
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { it.toReviewDto() }
                    .sortedByDescending { it.id } // Ordenar por ID en código (más reciente primero)
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean {
        val rootRef = firestoreService.collection("reviews").document(reviewId)
        val rootSnap = rootRef.get().await()
        if (!rootSnap.exists()) throw IllegalStateException("Review no encontrada: $reviewId")

        val data = rootSnap.data ?: emptyMap()
        val movieId = (data["movieId"] as? Number)?.toInt()
            ?: (data["movieId"] as? String)?.toIntOrNull()
            ?: throw IllegalStateException("movieId no encontrado en review $reviewId")
        val authorUid = data["userId"] as? String
            ?: throw IllegalStateException("userId no encontrado en review $reviewId")

        val likeDocRef = rootRef.collection("likes").document(userId)
        val mirrorLikeRef = firestoreService.collection("users")
            .document(userId).collection("likes").document(reviewId)

        val adding = firestoreService.runTransaction { tx ->
            val likeDoc = tx.get(likeDocRef)
            val addingLocal = !likeDoc.exists()
            val rootData = tx.get(rootRef).data ?: emptyMap()
            val currentLikes = (rootData["likes"] as? Number)?.toInt() ?: 0
            val newCount = if (addingLocal) currentLikes + 1 else maxOf(currentLikes - 1, 0)

            if (addingLocal) {
                tx.set(likeDocRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                tx.set(mirrorLikeRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
            } else {
                tx.delete(likeDocRef)
                tx.delete(mirrorLikeRef)
            }

            val update = mapOf("likes" to newCount, "updatedAt" to System.currentTimeMillis())
            tx.set(rootRef, update, SetOptions.merge())

            addingLocal
        }.await()

        val isNewLike = adding
        if (isNewLike && authorUid != userId && authorUid != currentUserId) {
            val actorProfile = getUserProfileById(currentUserId ?: userId)
            createNotification(
                targetUserId = authorUid,
                payload = mapOf(
                    "type" to "like",
                    "actorUserId" to (currentUserId ?: userId),
                    "actorName" to (actorProfile?.fullName ?: "Alguien"),
                    "reviewId" to reviewId,
                    "movieId" to movieId,
                    "seen" to false,
                    "createdAt" to System.currentTimeMillis()
                )
            )
        }

        return adding
    }

    // ---------------- NOTIFICACIONES ----------------
    override fun observeNotifications(userId: String): Flow<List<NotificationDto>> = callbackFlow {
        val ref = firestoreService.collection("users")
            .document(userId)
            .collection("notifications")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("Firestore", "Error observando notificaciones: ${err.message}")
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { it.toNotificationDto() }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun markNotificationSeen(userId: String, notificationId: String) {
        firestoreService.collection("users")
            .document(userId)
            .collection("notifications")
            .document(notificationId)
            .set(mapOf("seen" to true), SetOptions.merge())
            .await()
    }

    override suspend fun markAllNotificationsSeen(userId: String) {
        val ref = firestoreService.collection("users")
            .document(userId)
            .collection("notifications")
        val snap = ref.get().await()
        val batch = firestoreService.batch()
        snap.documents.forEach { d ->
            batch.set(d.reference, mapOf("seen" to true), SetOptions.merge())
        }
        batch.commit().await()
    }

    private suspend fun createNotification(targetUserId: String, payload: Map<String, Any>) {
        val doc = firestoreService.collection("users")
            .document(targetUserId)
            .collection("notifications")
            .document()
        val base = payload.toMutableMap()
        base["id"] = doc.id
        doc.set(base).await()
    }

    override suspend fun getAllMovies(): List<MovieDto> {
        val snap = firestoreService.collection("movies").get().await()
        return snap.documents.mapNotNull { it.toMovieDto() }
    }

    override suspend fun getMovieById(movieId: Int): MovieDto? {
        val doc = firestoreService.collection("movies").document(movieId.toString()).get().await()
        return doc.toMovieDto()
    }

    override suspend fun getMoviesByGenre(genre: String): List<MovieDto> {
        val all = getAllMovies()
        return if (genre == "Todos") all
        else all.filter { it.titulo.contains(genre, ignoreCase = true) || it.descripcion.contains(genre, ignoreCase = true) }
    }

    override suspend fun searchMovies(query: String): List<MovieDto> {
        val q = query.trim()
        if (q.isEmpty()) return getAllMovies()
        val all = getAllMovies()
        return all.filter {
            it.titulo.contains(q, true) || it.descripcion.contains(q, true)
        }
    }

    override suspend fun followUser(targetUserId: String): Boolean {
        val uid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (uid == targetUserId) return false
        
        firestoreService.collection("users").document(uid)
            .collection("following").document(targetUserId)
            .set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
        firestoreService.collection("users").document(targetUserId)
            .collection("followers").document(uid)
            .set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()

        val actorProfile = getUserProfileById(uid)
        createNotification(
            targetUserId = targetUserId,
            payload = mapOf(
                "type" to "follow",
                "actorUserId" to uid,
                "actorName" to (actorProfile?.fullName ?: "Alguien"),
                "seen" to false,
                "createdAt" to System.currentTimeMillis()
            )
        )
        return true
    }

    override suspend fun unfollowUser(targetUserId: String): Boolean {
        val uid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (uid == targetUserId) return false
        
        firestoreService.collection("users").document(uid)
            .collection("following").document(targetUserId).delete().await()
        firestoreService.collection("users").document(targetUserId)
            .collection("followers").document(uid).delete().await()
        return true
    }

    override suspend fun getFollowers(userId: String): List<UserDto> {
        val snap = firestoreService.collection("users").document(userId)
            .collection("followers").get().await()
        val ids = snap.documents.map { it.id }
        return ids.mapNotNull { id -> getUserProfileById(id) }
    }

    override suspend fun getFollowing(userId: String): List<UserDto> {
        val snap = firestoreService.collection("users").document(userId)
            .collection("following").get().await()
        val ids = snap.documents.map { it.id }
        return ids.mapNotNull { id -> getUserProfileById(id) }
    }
}

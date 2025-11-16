package com.example.ratingroom.data.datasource.impl

import android.util.Log
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
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

    override suspend fun getUserProfile(): Map<String, Any>? {
        val userId = currentUserId ?: return null
        val doc = firestoreService.collection("users").document(userId).get().await()
        return if (doc.exists()) doc.data else null
    }

    override suspend fun getUserProfileById(userId: String): Map<String, Any>? {
        val doc = firestoreService.collection("users").document(userId).get().await()
        return if (doc.exists()) doc.data else null
    }

    // ---------------- USUARIOS ----------------
    override suspend fun getAllUsers(): List<Map<String, Any>> {
        return try {
            val snap = firestoreService.collection("users").get().await()
            snap.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    this["uid"] = doc.id
                    if (!this.containsKey("email")) this["email"] = ""
                    if (!this.containsKey("fullName")) {
                        this["fullName"] = (this["displayName"] as? String).orEmpty()
                    }
                    if (!this.containsKey("username")) {
                        val email = (this["email"] as? String).orEmpty()
                        this["username"] =
                            if (email.contains("@")) email.substringBefore("@") else ""
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getAllUsers: ${e.message}", e)
            emptyList()
        }
    }

    // ---------------- RESEÑAS ----------------
    override suspend fun createReviewFanout(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): String {
        val userProfile = getUserProfileById(userId)
        val userName = userProfile?.get("fullName") as? String ?: "Usuario"
        val userImageUrl = userProfile?.get("profileImageUrl") as? String

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

        firestoreService.collection("users")
            .document(userId)
            .collection("reviews")
            .document(reviewDoc.id)
            .set(reviewData)
            .await()

        firestoreService.collection("movies")
            .document(movieId.toString())
            .collection("reviews")
            .document(reviewDoc.id)
            .set(reviewData)
            .await()

        return reviewDoc.id
    }

    override suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>> {
        val snapshot = firestoreService.collection("movies")
            .document(movieId.toString())
            .collection("reviews")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.toMutableMap()?.apply {
                if (this["id"] == null) this["id"] = doc.id
                if (this["likes"] == null) this["likes"] = 0
            }
        }
    }

    override suspend fun getReviewsByUser(userId: String): List<Map<String, Any>> {
        val snapshot = firestoreService.collection("users")
            .document(userId)
            .collection("reviews")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.data?.toMutableMap()?.apply {
                if (this["id"] == null) this["id"] = doc.id
                if (this["likes"] == null) this["likes"] = 0
            }
        }
    }

    override fun observeReviewsByMovie(movieId: Int): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = firestoreService.collection("movies")
            .document(movieId.toString())
            .collection("reviews")

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("Firestore", "Error observando reseñas: ${err.message}")
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        if (this["id"] == null) this["id"] = doc.id
                        if (this["likes"] == null) this["likes"] = 0
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeReviewsByUser(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = firestoreService.collection("users")
            .document(userId)
            .collection("reviews")

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e("Firestore", "Error observando reseñas de usuario: ${err.message}")
                return@addSnapshotListener
            }
            if (snap != null) {
                val list = snap.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        if (this["id"] == null) this["id"] = doc.id
                        if (this["likes"] == null) this["likes"] = 0
                    }
                }
                trySend(list)
            }
        }
        awaitClose { listener.remove() }
    }

    // ✅ Like toggle + notificación al autor
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

        val movieReviewRef = firestoreService.collection("movies")
            .document(movieId.toString()).collection("reviews").document(reviewId)
        val userReviewRef = firestoreService.collection("users")
            .document(authorUid).collection("reviews").document(reviewId)
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

            val update = mapOf("likes" to newCount)
            tx.set(rootRef, update, SetOptions.merge())
            tx.set(movieReviewRef, update, SetOptions.merge())
            tx.set(userReviewRef, update, SetOptions.merge())

            addingLocal
        }.await()

        // Crear notificación SOLO cuando se añade el like y no es auto-like
        if (adding && authorUid != userId) {
            createNotification(
                targetUserId = authorUid,
                payload = mapOf(
                    "type" to "like",
                    "actorUserId" to (currentUserId ?: userId),
                    "actorName" to (getUserProfile()?.get("fullName") as? String ?: "Alguien"),
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
    override fun observeNotifications(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
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
                val list = snap.documents.mapNotNull { d ->
                    d.data?.toMutableMap()?.apply { if (this["id"] == null) this["id"] = d.id }
                }
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

    // ---------------- PELÍCULAS ----------------
    override suspend fun getAllMovies(): List<Map<String, Any>> {
        return try {
            val snap = firestoreService.collection("movies").get().await()
            snap.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    if (this["id"] == null) this["id"] = doc.id.toIntOrNull() ?: 0
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getAllMovies: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getMovieById(movieId: Int): Map<String, Any>? {
        val doc = firestoreService.collection("movies").document(movieId.toString()).get().await()
        return if (doc.exists()) doc.data?.toMutableMap()?.apply { if (this["id"] == null) this["id"] = movieId } else null
    }

    override suspend fun getMoviesByGenre(genre: String): List<Map<String, Any>> {
        val all = getAllMovies()
        return if (genre == "Todos") all
        else all.filter { (it["genre"] as? String)?.equals(genre, ignoreCase = true) == true }
    }

    override suspend fun searchMovies(query: String): List<Map<String, Any>> {
        val q = query.trim()
        if (q.isEmpty()) return getAllMovies()
        val all = getAllMovies()
        return all.filter {
            val t = it["title"] as? String ?: ""
            val d = it["description"] as? String ?: ""
            val g = it["genre"] as? String ?: ""
            t.contains(q, true) || d.contains(q, true) || g.contains(q, true)
        }
    }

    // ---------------- SEGUIDORES / SEGUIDOS ----------------
    override suspend fun followUser(targetUserId: String): Boolean {
        val uid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (uid == targetUserId) return false
        return try {
            // seguir
            firestoreService.collection("users").document(uid)
                .collection("following").document(targetUserId)
                .set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()
            firestoreService.collection("users").document(targetUserId)
                .collection("followers").document(uid)
                .set(mapOf("timestamp" to FieldValue.serverTimestamp())).await()

            // notificación al seguido
            val actorName = getUserProfile()?.get("fullName") as? String ?: "Alguien"
            createNotification(
                targetUserId = targetUserId,
                payload = mapOf(
                    "type" to "follow",
                    "actorUserId" to uid,
                    "actorName" to actorName,
                    "seen" to false,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error followUser: ${e.message}", e)
            false
        }
    }

    override suspend fun unfollowUser(targetUserId: String): Boolean {
        val uid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (uid == targetUserId) return false
        return try {
            firestoreService.collection("users").document(uid)
                .collection("following").document(targetUserId).delete().await()
            firestoreService.collection("users").document(targetUserId)
                .collection("followers").document(uid).delete().await()
            true
        } catch (e: Exception) {
            Log.e("Firestore", "Error unfollowUser: ${e.message}", e)
            false
        }
    }

    override suspend fun getFollowers(userId: String): List<Map<String, Any>> {
        return try {
            val snap = firestoreService.collection("users").document(userId)
                .collection("followers").get().await()
            val ids = snap.documents.map { it.id }
            ids.mapNotNull { id ->
                getUserProfileById(id)?.toMutableMap()?.apply { this["uid"] = id }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getFollowers: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getFollowing(userId: String): List<Map<String, Any>> {
        return try {
            val snap = firestoreService.collection("users").document(userId)
                .collection("following").get().await()
            val ids = snap.documents.map { it.id }
            ids.mapNotNull { id ->
                getUserProfileById(id)?.toMutableMap()?.apply { this["uid"] = id }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getFollowing: ${e.message}", e)
            emptyList()
        }
    }

    // ---------------- FAVORITOS DE PELÍCULAS ----------------
    // ✅ Toggle favorito en película (similar a likes en reviews)
    override suspend fun toggleMovieFavorite(movieId: Int, userId: String): Boolean {
        val movieRef = firestoreService.collection("movies").document(movieId.toString())
        val movieSnap = movieRef.get().await()
        if (!movieSnap.exists()) throw IllegalStateException("Película no encontrada: $movieId")

        // Subcolección de favoritos en la película: movies/{movieId}/favorites/{userId}
        val favoriteDocRef = movieRef.collection("favorites").document(userId)
        // Espejo en el usuario: users/{userId}/favoriteMovies/{movieId}
        val mirrorFavoriteRef = firestoreService.collection("users")
            .document(userId).collection("favoriteMovies").document(movieId.toString())

        val adding = firestoreService.runTransaction { tx ->
            val favoriteDoc = tx.get(favoriteDocRef)
            val addingLocal = !favoriteDoc.exists()
            val movieData = tx.get(movieRef).data ?: emptyMap()
            val currentFavorites = (movieData["favoritesCount"] as? Number)?.toInt() ?: 0
            val newCount = if (addingLocal) currentFavorites + 1 else maxOf(currentFavorites - 1, 0)

            if (addingLocal) {
                // Agregar a favoritos
                tx.set(favoriteDocRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                tx.set(mirrorFavoriteRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
            } else {
                // Quitar de favoritos
                tx.delete(favoriteDocRef)
                tx.delete(mirrorFavoriteRef)
            }

            // Actualizar contador en el documento de la película
            val update = mapOf("favoritesCount" to newCount)
            tx.set(movieRef, update, SetOptions.merge())

            addingLocal
        }.await()

        return adding
    }

    override suspend fun isMovieFavoriteByUser(movieId: Int, userId: String): Boolean {
        return try {
            val favoriteDoc = firestoreService.collection("movies")
                .document(movieId.toString())
                .collection("favorites")
                .document(userId)
                .get()
                .await()
            favoriteDoc.exists()
        } catch (e: Exception) {
            Log.e("Firestore", "Error isMovieFavoriteByUser: ${e.message}", e)
            false
        }
    }

    override suspend fun getFavoriteMoviesCount(movieId: Int): Int {
        return try {
            val movieDoc = firestoreService.collection("movies")
                .document(movieId.toString())
                .get()
                .await()
            (movieDoc.data?.get("favoritesCount") as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e("Firestore", "Error getFavoriteMoviesCount: ${e.message}", e)
            0
        }
    }

    override suspend fun getUserFavoriteMovies(userId: String): List<Map<String, Any>> {
        return try {
            // Obtener los IDs de películas favoritas del usuario
            val favoritesSnap = firestoreService.collection("users")
                .document(userId)
                .collection("favoriteMovies")
                .get()
                .await()
            
            val movieIds = favoritesSnap.documents.map { it.id }
            
            // Obtener los datos completos de cada película
            val movies = movieIds.mapNotNull { movieId ->
                try {
                    val movieDoc = firestoreService.collection("movies")
                        .document(movieId)
                        .get()
                        .await()
                    
                    if (movieDoc.exists()) {
                        movieDoc.data?.toMutableMap()?.apply {
                            if (this["id"] == null) this["id"] = movieId.toIntOrNull() ?: 0
                        }
                    } else null
                } catch (e: Exception) {
                    Log.e("Firestore", "Error obteniendo película $movieId: ${e.message}", e)
                    null
                }
            }
            
            movies
        } catch (e: Exception) {
            Log.e("Firestore", "Error getUserFavoriteMovies: ${e.message}", e)
            emptyList()
        }
    }
}

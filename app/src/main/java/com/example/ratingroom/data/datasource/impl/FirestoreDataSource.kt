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

    // ---------------- Perfil ----------------

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

        if (updates.isNotEmpty()) {
            val docRef = firestoreService.collection("users").document(userId)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                docRef.update(updates).await()
            } else {
                updates["createdAt"] = System.currentTimeMillis()
                docRef.set(updates).await()
            }
        }
    }

    override suspend fun getUserProfile(): Map<String, Any>? {
        val userId = currentUserId ?: return null
        val document = firestoreService.collection("users")
            .document(userId)
            .get()
            .await()
        return if (document.exists()) document.data else null
    }

    override suspend fun getUserProfileById(userId: String): Map<String, Any>? {
        val document = firestoreService.collection("users")
            .document(userId)
            .get()
            .await()
        return if (document.exists()) document.data else null
    }

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

        firestoreService.collection("users")
            .document(userId)
            .set(data)
            .await()
    }

    // ---------------- Reseñas ----------------

    override suspend fun createReviewFanout(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): String {
        // 1) Obtener información del usuario para desnormalizar
        val userProfile = getUserProfileById(userId)
        val userName = userProfile?.get("fullName") as? String ?: "Usuario"
        val userImageUrl = userProfile?.get("profileImageUrl") as? String
        
        // 2) Crea documento raíz en /reviews
        val reviewDoc = firestoreService.collection("reviews").document()
        val now = System.currentTimeMillis()
        val reviewData = mapOf(
            "id" to reviewDoc.id,
            "userId" to userId,
            "movieId" to movieId,
            "rating" to rating,
            "text" to text,
            "createdAt" to now,
            "updatedAt" to now,
            // 🎯 Desnormalización: información del usuario
            "userName" to userName,
            "userImageUrl" to (userImageUrl ?: "")
        )

        // Escritura inicial
        reviewDoc.set(reviewData).await()

        // 3) Fanout a /users/{uid}/reviews/{reviewId}
        firestoreService.collection("users")
            .document(userId)
            .collection("reviews")
            .document(reviewDoc.id)
            .set(reviewData)
            .await()

        // 4) Fanout a /movies/{movieId}/reviews/{reviewId}
        firestoreService.collection("movies")
            .document(movieId.toString())
            .set(mapOf("updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
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
            }
        }
    }
    
    override fun observeReviewsByMovie(movieId: Int): Flow<List<Map<String, Any>>> = callbackFlow {
        val reviewsRef = firestoreService.collection("movies")
            .document(movieId.toString())
            .collection("reviews")
            
        val subscription = reviewsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreDataSource", "Error observando reseñas: ${error.message}", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val reviews = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        if (this["id"] == null) this["id"] = doc.id
                    }
                }
                trySend(reviews)
            }
        }
        
        awaitClose { subscription.remove() }
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
            }
        }
    }
    
    override fun observeReviewsByUser(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val reviewsRef = firestoreService.collection("users")
            .document(userId)
            .collection("reviews")
            
        val subscription = reviewsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreDataSource", "Error observando reseñas de usuario: ${error.message}", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val reviews = snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        if (this["id"] == null) this["id"] = doc.id
                    }
                }
                trySend(reviews)
            }
        }
        
        awaitClose { subscription.remove() }
    }

    override suspend fun sendOrDeleteLike(reviewId: String, userId: String): Boolean {
        val reviewRef = firestoreService.collection("reviews").document(reviewId)
        val likesRef = reviewRef.collection("likes").document(userId)

        return firestoreService.runTransaction { transaction ->
            val likeDoc = transaction.get(likesRef)
            
            if (likeDoc.exists()) {
                transaction.delete(likesRef)
                transaction.set(reviewRef, mapOf("likes" to FieldValue.increment(-1)), com.google.firebase.firestore.SetOptions.merge())
                false
            } else {
                transaction.set(likesRef, mapOf("timestamp" to FieldValue.serverTimestamp()))
                transaction.set(reviewRef, mapOf("likes" to FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge())
                true
            }
        }.await()
    }

    // ---------------- Películas ----------------

    override suspend fun getAllMovies(): List<Map<String, Any>> {
        try {
            Log.d("FirestoreDataSource", "🔥 Obteniendo películas desde Firestore...")
            val snapshot = firestoreService.collection("movies")
                .get()
                .await()

            Log.d("FirestoreDataSource", "🔥 Firestore devolvió ${snapshot.documents.size} documentos")
            
            val movies = snapshot.documents.mapNotNull { doc ->
                doc.data?.toMutableMap()?.apply {
                    // Asegurar que el ID esté presente
                    if (this["id"] == null) this["id"] = doc.id.toIntOrNull() ?: 0
                    Log.d("FirestoreDataSource", "🔥 Película: ${this["title"]} (ID: ${this["id"]})")
                }
            }
            
            Log.d("FirestoreDataSource", "🔥 Procesadas ${movies.size} películas")
            return movies
        } catch (e: Exception) {
            Log.e("FirestoreDataSource", "🔥 Error obteniendo películas desde Firestore: ${e.message}", e)
            return emptyList()
        }
    }

    override suspend fun getMovieById(movieId: Int): Map<String, Any>? {
        val doc = firestoreService.collection("movies")
            .document(movieId.toString())
            .get()
            .await()

        return if (doc.exists()) {
            doc.data?.toMutableMap()?.apply {
                if (this["id"] == null) this["id"] = movieId
            }
        } else {
            null
        }
    }

    override suspend fun getMoviesByGenre(genre: String): List<Map<String, Any>> {
        val allMovies = getAllMovies()
        return if (genre == "Todos") {
            allMovies
        } else {
            allMovies.filter { movie ->
                val movieGenre = movie["genre"] as? String ?: ""
                movieGenre.equals(genre, ignoreCase = true)
            }
        }
    }

    override suspend fun searchMovies(query: String): List<Map<String, Any>> {
        val q = query.trim()
        if (q.isEmpty()) return getAllMovies()
        
        val allMovies = getAllMovies()
        return allMovies.filter { movie ->
            val title = movie["title"] as? String ?: ""
            val description = movie["description"] as? String ?: ""
            val genre = movie["genre"] as? String ?: ""
            
            title.contains(q, ignoreCase = true) ||
            description.contains(q, ignoreCase = true) ||
            genre.contains(q, ignoreCase = true)
        }
    }

    // Aux opcional si lo necesitas
    suspend fun deleteUserDocument(userId: String) {
        firestoreService.collection("users").document(userId).delete().await()
    }
    
    // ---------------- Seguidores y Seguidos ----------------
    
    override suspend fun followUser(targetUserId: String): Boolean {
        val currentUid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (currentUid == targetUserId) return false // No se puede seguir a sí mismo
        
        try {
            // 1. Añadir a la lista de "following" del usuario actual
            firestoreService.collection("users")
                .document(currentUid)
                .collection("following")
                .document(targetUserId)
                .set(mapOf(
                    "timestamp" to FieldValue.serverTimestamp()
                ))
                .await()
                
            // 2. Añadir a la lista de "followers" del usuario objetivo
            firestoreService.collection("users")
                .document(targetUserId)
                .collection("followers")
                .document(currentUid)
                .set(mapOf(
                    "timestamp" to FieldValue.serverTimestamp()
                ))
                .await()
                
            // 3. Actualizar contadores en ambos documentos
            firestoreService.collection("users")
                .document(currentUid)
                .update("followingCount", FieldValue.increment(1))
                .await()
                
            firestoreService.collection("users")
                .document(targetUserId)
                .update("followersCount", FieldValue.increment(1))
                .await()
                
            return true
        } catch (e: Exception) {
            Log.e("FirestoreDataSource", "Error al seguir usuario: ${e.message}", e)
            return false
        }
    }
    
    override suspend fun unfollowUser(targetUserId: String): Boolean {
        val currentUid = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        if (currentUid == targetUserId) return false // No tiene sentido
        
        try {
            // 1. Eliminar de la lista de "following" del usuario actual
            firestoreService.collection("users")
                .document(currentUid)
                .collection("following")
                .document(targetUserId)
                .delete()
                .await()
                
            // 2. Eliminar de la lista de "followers" del usuario objetivo
            firestoreService.collection("users")
                .document(targetUserId)
                .collection("followers")
                .document(currentUid)
                .delete()
                .await()
                
            // 3. Actualizar contadores en ambos documentos
            firestoreService.collection("users")
                .document(currentUid)
                .update("followingCount", FieldValue.increment(-1))
                .await()
                
            firestoreService.collection("users")
                .document(targetUserId)
                .update("followersCount", FieldValue.increment(-1))
                .await()
                
            return true
        } catch (e: Exception) {
            Log.e("FirestoreDataSource", "Error al dejar de seguir usuario: ${e.message}", e)
            return false
        }
    }
    
    override suspend fun getFollowers(userId: String): List<Map<String, Any>> {
        try {
            val followersSnapshot = firestoreService.collection("users")
                .document(userId)
                .collection("followers")
                .get()
                .await()
                
            val followerIds = followersSnapshot.documents.map { it.id }
            
            // Obtener los perfiles completos de cada seguidor
            return followerIds.mapNotNull { followerId ->
                getUserProfileById(followerId)?.let { profile ->
                    profile.toMutableMap().apply {
                        this["uid"] = followerId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreDataSource", "Error al obtener seguidores: ${e.message}", e)
            return emptyList()
        }
    }
    
    override suspend fun getFollowing(userId: String): List<Map<String, Any>> {
        try {
            val followingSnapshot = firestoreService.collection("users")
                .document(userId)
                .collection("following")
                .get()
                .await()
                
            val followingIds = followingSnapshot.documents.map { it.id }
            
            // Obtener los perfiles completos de cada usuario seguido
            return followingIds.mapNotNull { followingId ->
                getUserProfileById(followingId)?.let { profile ->
                    profile.toMutableMap().apply {
                        this["uid"] = followingId
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreDataSource", "Error al obtener usuarios seguidos: ${e.message}", e)
            return emptyList()
        }
    }
}

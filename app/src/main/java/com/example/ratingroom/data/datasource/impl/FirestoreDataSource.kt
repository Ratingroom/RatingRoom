package com.example.ratingroom.data.datasource.impl

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        profileImageUrl: String?
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
        // 1) Crea documento raíz en /reviews
        val reviewDoc = firestoreService.collection("reviews").document()
        val now = System.currentTimeMillis()
        val reviewData = mapOf(
            "id" to reviewDoc.id,
            "userId" to userId,
            "movieId" to movieId,
            "rating" to rating,
            "text" to text,
            "createdAt" to now,
            "updatedAt" to now
        )

        // Escritura inicial
        reviewDoc.set(reviewData).await()

        // 2) Fanout a /users/{uid}/reviews/{reviewId}
        firestoreService.collection("users")
            .document(userId)
            .collection("reviews")
            .document(reviewDoc.id)
            .set(reviewData)
            .await()

        // 3) Fanout a /movies/{movieId}/reviews/{reviewId}
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

    // Aux opcional si lo necesitas
    suspend fun deleteUserDocument(userId: String) {
        firestoreService.collection("users").document(userId).delete().await()
    }
}

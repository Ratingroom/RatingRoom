package com.example.ratingroom.data.datasource.impl

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreDataSourceImpl @Inject constructor(
    private val firestoreService: FirebaseFirestore,
    private val authService: FirebaseAuth
) : FirestoreDataSource {

    private val currentUserId: String?
        get() = authService.currentUser?.uid

    // ===== Perfil =====
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

        val docRef = firestoreService.collection("users").document(userId)
        val docSnapshot = docRef.get().await()
        if (docSnapshot.exists()) {
            docRef.update(updates).await()
        } else {
            docRef.set(updates).await()
        }
    }

    override suspend fun getUserProfile(): Map<String, Any>? {
        val userId = currentUserId ?: return null
        val snap = firestoreService.collection("users").document(userId).get().await()
        return if (snap.exists()) snap.data else null
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

        firestoreService.collection("users").document(userId).set(data).await()
    }

    // ===== Reseñas =====
    override suspend fun createReview(
        userId: String,
        movieId: Int,
        rating: Int,
        text: String
    ): Map<String, Any> {
        val data = hashMapOf<String, Any>(
            "userId" to userId,
            "movieId" to movieId,
            "rating" to rating,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val ref = firestoreService.collection("reviews").add(data).await()
        val saved = ref.get().await()
        return saved.data ?: emptyMap()
    }

    override suspend fun getReviewsByUser(userId: String): List<Map<String, Any>> {
        val q = firestoreService.collection("reviews")
            .whereEqualTo("userId", userId)
            .get()
            .await()
        return q.documents.mapNotNull { it.data }
    }

    override suspend fun getReviewsByMovie(movieId: Int): List<Map<String, Any>> {
        val q = firestoreService.collection("reviews")
            .whereEqualTo("movieId", movieId)
            .get()
            .await()
        return q.documents.mapNotNull { it.data }
    }
}

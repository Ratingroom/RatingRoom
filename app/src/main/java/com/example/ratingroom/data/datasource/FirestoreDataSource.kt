package com.example.ratingroom.data.datasource

interface FirestoreDataSource {
    suspend fun getUserProfile(): Map<String, Any>?
    suspend fun updateUserProfile(
        displayName: String?,
        email: String?,
        biography: String?,
        location: String?,
        favoriteGenre: String?,
        birthdate: String?,
        website: String?,
        profileImageUrl: String?
    )
    suspend fun createUserDocument(
        userId: String,
        email: String,
        fullName: String? = null,
        favoriteGenre: String? = null,
        birthYear: String? = null
    )
}
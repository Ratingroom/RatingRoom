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
}
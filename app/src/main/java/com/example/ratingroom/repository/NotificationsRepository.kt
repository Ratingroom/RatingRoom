package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.NotificationDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val ds: FirestoreDataSource,
    private val authRepository: AuthRepository
) {
    fun observeMyNotifications(): Flow<List<NotificationDto>> {
        val uid = authRepository.currentUser?.uid ?: ""
        return ds.observeNotifications(uid).map { list ->
            list.map { mapNotificationFromFirestore(it) }
        }
    }

    suspend fun markSeen(notificationId: String) {
        val uid = authRepository.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")
        ds.markNotificationSeen(uid, notificationId)
    }

    suspend fun markAllSeen() {
        val uid = authRepository.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado")
        ds.markAllNotificationsSeen(uid)
    }

    private fun mapNotificationFromFirestore(data: Map<String, Any>): NotificationDto {
        return NotificationDto(
            id = data["id"] as? String ?: "",
            type = data["type"] as? String ?: "",
            actorUserId = data["actorUserId"] as? String ?: "",
            actorName = data["actorName"] as? String,
            reviewId = data["reviewId"] as? String,
            movieId = (data["movieId"] as? Number)?.toInt(),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            seen = data["seen"] as? Boolean ?: false
        )
    }
}
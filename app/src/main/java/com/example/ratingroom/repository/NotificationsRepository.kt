package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsRepository @Inject constructor(
    private val ds: FirestoreDataSource,
    private val authRepository: AuthRepository
) {
    fun observeMyNotifications(): Flow<List<Map<String, Any>>> {
        val uid = authRepository.currentUser?.uid ?: ""
        return ds.observeNotifications(uid)
    }

    suspend fun markSeen(notificationId: String) {
        val uid = authRepository.currentUser?.uid ?: return
        ds.markNotificationSeen(uid, notificationId)
    }

    suspend fun markAllSeen() {
        val uid = authRepository.currentUser?.uid ?: return
        ds.markAllNotificationsSeen(uid)
    }
}

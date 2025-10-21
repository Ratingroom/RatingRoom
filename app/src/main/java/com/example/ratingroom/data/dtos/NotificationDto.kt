package com.example.ratingroom.data.dtos

data class NotificationDto(
    val id: String = "",
    val type: String = "",
    val actorUserId: String = "",
    val actorName: String? = null,
    val reviewId: String? = null,
    val movieId: Int? = null,
    val createdAt: Long = 0L,
    val seen: Boolean = false
)

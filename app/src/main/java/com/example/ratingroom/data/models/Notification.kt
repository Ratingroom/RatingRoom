package com.example.ratingroom.data.models

data class Notification(
    val id: String,
    val type: String,              // "like" | "follow"
    val actorUserId: String,
    val actorName: String?,
    val reviewId: String? = null,
    val movieId: Int? = null,
    val createdAt: Long,
    val seen: Boolean
)

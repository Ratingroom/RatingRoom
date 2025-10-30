package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Friend

// Modelo UI simple para pruebas de mapeo
data class FriendUi(
    val id: Int,
    val name: String,
    val username: String,
    val isOnline: Boolean,
    val averageRating: Double,
    val mutualFriends: Int
)

fun Friend.toUi(): FriendUi = FriendUi(
    id = id,
    name = name,
    username = username,
    isOnline = isOnline,
    averageRating = averageRating,
    mutualFriends = mutualFriends
)

fun List<Friend>.toUiList(): List<FriendUi> = this.map { it.toUi() }
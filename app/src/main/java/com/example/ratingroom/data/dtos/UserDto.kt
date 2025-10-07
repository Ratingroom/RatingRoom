package com.example.ratingroom.data.dtos

data class UserDto(
    val id: Int,
    val displayName: String,
    val email: String,
    val biography: String = "",
    val location: String = "",
    val favoriteGenre: String = "",
    val birthdate: String = "",
    val website: String = "",
    val profileImageUrl: String? = null
)

data class CreateUserDto(
    val displayName: String,
    val email: String,
    val biography: String = "",
    val location: String = "",
    val favoriteGenre: String = "",
    val birthdate: String = "",
    val website: String = "",
    val profileImageUrl: String? = null
)

data class UpdateUserDto(
    val displayName: String? = null,
    val email: String? = null,
    val biography: String? = null,
    val location: String? = null,
    val favoriteGenre: String? = null,
    val birthdate: String? = null,
    val website: String? = null,
    val profileImageUrl: String? = null
)

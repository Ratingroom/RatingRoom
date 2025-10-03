package com.example.ratingroom.data.models

data class ApiResponse<T>(
    val success: Boolean,
    val data: T
)

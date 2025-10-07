package com.example.ratingroom.data.dtos


data class ApiResponse<T>(
    val success: Boolean,
    val data: T?
)

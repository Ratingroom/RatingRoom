package com.example.ratingroom.data.dtos

data class CommentDto(
    val id: Int,
    val texto: String,
    val usuario_id: Int,
    val review_id: Int
)

data class CreateCommentDto(
    val texto: String,
    val review_id: Int
)

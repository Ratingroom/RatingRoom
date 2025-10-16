package com.example.ratingroom.ui.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReviewCard(
    title: String,
    rating: Int,                // 0..5
    excerpt: String,
    timeAgo: String,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    likes: Int = 0,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Row {
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (i < rating) cs.tertiary else cs.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = excerpt,
                        color = cs.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(timeAgo, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                
                // Action buttons
                if (onEdit != null || onDelete != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        onEdit?.let { editAction ->
                            IconButton(onClick = editAction) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar reseña",
                                    tint = cs.primary
                                )
                            }
                        }
                        onDelete?.let { deleteAction ->
                            IconButton(onClick = deleteAction) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar reseña",
                                    tint = cs.error
                                )
                            }
                        }
                    }
                }
            }
            
                    // Botón de Like
            if (onLikeClick != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isLiked) "Quitar like" else "Dar like",
                            tint = if (isLiked) cs.error else cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (likes > 0) {
                        Text(
                            text = likes.toString(),
                            color = if (isLiked) cs.error else cs.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

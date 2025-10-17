package com.example.ratingroom.ui.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
 ) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null, tint = cs.error)
        Spacer(Modifier.height(8.dp))
        Text(text = message, color = cs.error)
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            ElevatedButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text("Reintentar")
            }
        }
    }
}
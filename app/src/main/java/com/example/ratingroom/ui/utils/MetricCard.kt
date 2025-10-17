package com.example.ratingroom.ui.utils

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricCard(
    icon: ImageVector,
    number: String,
    label: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = cs.surface,
            contentColor = cs.onSurface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Spacer(Modifier.height(6.dp))
            Text(number, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(label, color = cs.onSurfaceVariant)
        }
    }
}

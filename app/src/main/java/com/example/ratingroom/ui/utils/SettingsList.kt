package com.example.ratingroom.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

data class SettingsItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color? = null
)

@Composable
fun SettingsList(
    items: List<SettingsItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item.id) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon, 
                    contentDescription = null, 
                    tint = item.tint ?: cs.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.label, 
                    color = item.tint ?: cs.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.ChevronRight, 
                    contentDescription = null, 
                    tint = cs.onSurfaceVariant
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

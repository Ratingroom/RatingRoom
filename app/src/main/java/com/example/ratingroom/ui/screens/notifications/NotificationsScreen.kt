package com.example.ratingroom.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ratingroom.data.models.Notification

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsRoute(
    onBack: () -> Unit,
    vm: NotificationsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.markAllSeen() }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Marcar todas como leídas"
                        )
                    }
                }
            )
        }
    ) { inner ->
        when {
            ui.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ui.items) { n ->
                    NotificationItem(
                        n = n,
                        onClick = { vm.markSeen(n.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    n: Notification,
    onClick: () -> Unit
) {
    val cardColors =
        if (n.seen) CardDefaults.elevatedCardColors()
        else CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = cardColors
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val icon = when (n.type) {
                "like" -> Icons.Default.Favorite
                "follow" -> Icons.Default.PersonAdd
                else -> Icons.Default.PersonAdd
            }
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (n.type) {
                        "like" -> "${n.actorName ?: "Alguien"} le dio like a tu reseña"
                        "follow" -> "${n.actorName ?: "Alguien"} empezó a seguirte"
                        else -> "Nueva actividad"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                if (n.movieId != null && n.type == "like") {
                    Text(
                        text = "Película ID: ${n.movieId}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!n.seen) {
                AssistChip(onClick = onClick, label = { Text("Marcar leído") })
            }
        }
    }
}

package com.example.ratingroom.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ratingroom.R
import com.example.ratingroom.ui.screens.profile.ProfileData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    title: String,
    onMenuClick: () -> Unit,
    profileData: ProfileData? = null
) {
    // Debug: Verificar qué datos está recibiendo
    println("ModernTopBar: profileData = $profileData")
    println("ModernTopBar: name = ${profileData?.name}")
    println("ModernTopBar: email = ${profileData?.email}")
    println("ModernTopBar: imageUrl = ${profileData?.profileImageUrl}")
    
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(id = R.drawable.logoratingroom),
                    contentDescription = stringResource(id = R.string.content_desc_logo),
                    tint = Color.Unspecified
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Avatar del usuario en la barra superior
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                AvatarInitials(
                    initials = profileData?.name?.take(2)?.uppercase() ?: "U",
                    imageUrl = profileData?.profileImageUrl,
                    size = 40.dp
                )
            }
        }
    }
}

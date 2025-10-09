package com.example.ratingroom.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.ratingroom.R
import com.example.ratingroom.ui.screens.profile.ProfileData

@Composable
fun ProfileMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onProfileClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    profileData: ProfileData? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
                .clickable { onExpandedChange(true) },
            contentAlignment = Alignment.Center
        ) {
            AvatarInitials(
                initials = profileData?.name?.take(2)?.uppercase() ?: "U",
                imageUrl = profileData?.profileImageUrl,
                size = 32.dp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.main_menu_profile)) },
                onClick = onProfileClick
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.friends_title)) },
                onClick = onFriendsClick
            )
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.main_menu_logout)) },
                onClick = onLogoutClick
            )
        }
    }
}

package com.example.ratingroom.ui.utils

import androidx.compose.animation.core.*
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ratingroom.R
import com.example.ratingroom.navigation.Screen
import com.example.ratingroom.ui.screens.profile.ProfileData
import com.example.ratingroom.ui.utils.AvatarInitials

@Composable
fun ModernNavigationDrawer(
    isOpen: Boolean,
    onToggle: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    profileData: ProfileData? = null,
    modifier: Modifier = Modifier
) {
    val animatedWidth by animateDpAsState(
        targetValue = if (isOpen) 280.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "drawer_width"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = if (isOpen) 50 else 0
        ),
        label = "drawer_alpha"
    )

    if (isOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable { onToggle() }
                .zIndex(1f)
        )
    }

    Box(
        modifier = modifier
            .width(animatedWidth)
            .fillMaxHeight()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Top + WindowInsetsSides.Bottom))
            .zIndex(2f)
    ) {
        if (animatedWidth > 0.dp) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    ),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    DrawerHeader(
                        alpha = animatedAlpha,
                        onClose = onToggle,
                        profileData = profileData
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    NavigationItems(
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        alpha = animatedAlpha
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    LogoutSection(
                        onLogout = onLogout,
                        alpha = animatedAlpha
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    alpha: Float,
    onClose: () -> Unit,
    profileData: ProfileData? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logoratingroom),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "RatingRoom",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AvatarInitials(
            initials = profileData?.name?.take(2)?.uppercase() ?: "U",
            imageUrl = profileData?.profileImageUrl,
            size = 64.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = profileData?.name ?: "Usuario",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = profileData?.email ?: "usuario@email.com",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NavigationItems(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    alpha: Float
) {
    Column(
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        Screen.mainScreens.forEach { screen ->
            NavigationDrawerItem(
                screen = screen,
                isSelected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NavigationDrawerItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            screen.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = screen.title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = screen.title,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
private fun LogoutSection(
    onLogout: () -> Unit,
    alpha: Float
) {
    Column(
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onLogout() },
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    tint = Color(0xFFD32F2F), // Rojo brillante
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Cerrar sesión",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD32F2F) // Rojo brillante
                )
            }
        }
    }
}

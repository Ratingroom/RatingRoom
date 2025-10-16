package com.example.ratingroom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ratingroom.navigation.Screen
import com.example.ratingroom.ui.screens.app.MainViewModel
import com.example.ratingroom.ui.screens.favorites.FavoritesScreen
import com.example.ratingroom.ui.screens.forgotpassword.ForgotPasswordScreen
import com.example.ratingroom.ui.screens.friends.FriendsScreen
import com.example.ratingroom.ui.screens.list.ListScreen
import com.example.ratingroom.ui.screens.login.LoginScreen
import com.example.ratingroom.ui.screens.main.MainMenuScreen
import com.example.ratingroom.ui.screens.moviedetail.MovieDetailRoute
import com.example.ratingroom.ui.screens.profile.EditProfileScreen
import com.example.ratingroom.ui.screens.profile.ProfileScreen
import com.example.ratingroom.ui.screens.profile.ProfileViewModel
import com.example.ratingroom.ui.screens.friends.FriendRoute // ⬅️ NUEVO
import com.example.ratingroom.ui.screens.register.RegisterScreen
import com.example.ratingroom.ui.screens.reviews.ReviewsScreen
import com.example.ratingroom.ui.screens.settings.SettingsScreen
import com.example.ratingroom.ui.screens.synopsis.SynopsisScreen
import com.example.ratingroom.ui.theme.RatingRoomTheme
import com.example.ratingroom.ui.utils.GradientBackground
import com.example.ratingroom.ui.utils.ModernNavigationDrawer
import com.example.ratingroom.ui.utils.ModernTopBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Estado de autenticación pasado desde SplashActivity
        val isUserLoggedIn = intent.getBooleanExtra(SplashActivity.EXTRA_IS_USER_LOGGED_IN, false)

        setContent {
            RatingRoomTheme {
                RatingRoomApp(isUserLoggedIn = isUserLoggedIn)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingRoomApp(isUserLoggedIn: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // VM principal
    val mainViewModel: MainViewModel = hiltViewModel()
    val mainUiState by mainViewModel.uiState.collectAsState()

    // Datos del usuario para drawer/topbar
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileUiState by profileViewModel.uiState.collectAsState()

    // ✅ Estado de autenticación reactivo
    var currentAuthState by remember { mutableStateOf(isUserLoggedIn) }

    // Recargar perfil al salir de pantallas de auth
    LaunchedEffect(currentRoute) {
        if (
            currentRoute != Screen.Login.route &&
            currentRoute != Screen.Register.route &&
            currentRoute != Screen.ForgotPassword.route &&
            profileUiState.profileData == null &&
            !profileUiState.isLoading
        ) {
            profileViewModel.loadProfile()
        }
    }

    val navigateToScreen: (String) -> Unit = { route ->
        navController.navigate(route) { launchSingleTop = true }
        mainViewModel.updateDrawerState(false)
    }

    val navigateBack: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) {
            navController.navigate(Screen.MainMenu.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
        mainViewModel.updateDrawerState(false)
    }

    val isAuthScreen = mainViewModel.isAuthScreen(currentRoute ?: "")
    val showTopBar = !isAuthScreen
    val showDrawer = !isAuthScreen
    val currentTitle = mainViewModel.getCurrentTitle(currentRoute ?: "")

    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    if (showTopBar) {
                        ModernTopBar(
                            title = currentTitle,
                            onMenuClick = { mainViewModel.toggleDrawer() },
                            profileData = profileUiState.profileData
                        )
                    }
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->

                // ✅ Iniciar según estado de autenticación actual
                val startDestination = if (currentAuthState) {
                    Screen.MainMenu.route
                } else {
                    Screen.Login.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    // ---------- AUTH ----------
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginClick = { _, _ ->
                                // ✅ Actualizar estado de autenticación
                                currentAuthState = true
                                navController.navigate(Screen.MainMenu.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            },
                            onForgotPasswordClick = { navigateToScreen(Screen.ForgotPassword.route) },
                            onRegisterClick = { navigateToScreen(Screen.Register.route) }
                        )
                    }
                    composable(Screen.Register.route) {
                        RegisterScreen(
                            onRegisterClick = { _, _, _, _, _, _ ->
                                // ✅ Actualizar estado de autenticación después del registro
                                currentAuthState = true
                                navigateBack()
                            },
                            onLoginClick = { navigateBack() },
                            onBackClick = { navigateBack() }
                        )
                    }
                    composable(Screen.ForgotPassword.route) {
                        ForgotPasswordScreen(
                            onSendRecoveryClick = { _ -> navigateBack() },
                            onBackToLoginClick = { navigateBack() }
                        )
                    }

                    // ---------- PRINCIPALES ----------
                    composable(Screen.MainMenu.route) {
                        MainMenuScreen(
                            onMovieClick = { id -> navigateToScreen(Screen.MovieDetail.createRoute(id)) }
                        )
                    }

                    composable(Screen.Profile.route) {
                        ProfileScreen(
                            onBackClick = { /* handled by top bar global */ },
                            onEditClick = { navigateToScreen(Screen.EditProfile.route) },
                            onLogoutClick = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.EditProfile.route) {
                        EditProfileScreen(
                            onSave = { navigateBack() },
                            onBackClick = { navigateBack() }
                        )
                    }

                    // Friends -> abre el perfil del usuario clickeado
                    composable(Screen.Friends.route) {
                        FriendsScreen(
                            onBack = navigateBack,
                            onUserClick = { userId ->
                                navigateToScreen(Screen.Friend.createRoute(userId))
                            }
                        )
                    }

                    composable(Screen.List.route) {
                        ListScreen(
                            onMovieClick = { movieId ->
                                navigateToScreen(Screen.MovieDetail.createRoute(movieId))
                            }
                        )
                    }

                    composable(Screen.Reviews.route) { ReviewsScreen(onBack = navigateBack) }

                    composable(Screen.Favorites.route) {
                        FavoritesScreen(
                            onBack = navigateBack,
                            onMovieClick = { movieId ->
                                navigateToScreen(Screen.MovieDetail.createRoute(movieId))
                            }
                        )
                    }

                    composable(Screen.Settings.route) { SettingsScreen(onBack = navigateBack) }

                    // ---------- DETALLE ----------
                    composable(
                        route = Screen.MovieDetail.route,
                        arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                        MovieDetailRoute(movieId = movieId, onBack = navigateBack)
                    }
                    composable(
                        route = Screen.Synopsis.route,
                        arguments = listOf(navArgument("movieId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
                        SynopsisScreen(movieId = movieId, onBackClick = navigateBack)
                    }

                    // ---------- FRIEND (perfil de otro usuario) ----------
                    composable(
                        route = Screen.Friend.route, // "friend/{userId}"
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("userId") ?: return@composable
                        FriendRoute(userId = uid, onBack = navigateBack)
                    }
                }
            }

            if (showDrawer) {
                ModernNavigationDrawer(
                    isOpen = mainUiState.isDrawerOpen,
                    onToggle = { mainViewModel.toggleDrawer() },
                    currentRoute = currentRoute,
                    onNavigate = { route -> navigateToScreen(route) },
                    onLogout = {
                        profileViewModel.logout()
                        // ✅ Actualizar estado de autenticación
                        currentAuthState = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                        mainViewModel.updateDrawerState(false)
                    },
                    profileData = profileUiState.profileData
                )
            }
        }
    }
}

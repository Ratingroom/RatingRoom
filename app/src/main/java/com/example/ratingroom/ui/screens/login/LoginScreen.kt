package com.example.ratingroom.ui.screens.login

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ratingroom.R
import com.example.ratingroom.ui.theme.*
import com.example.ratingroom.ui.utils.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit = { _, _ -> },
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    LoginScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onLoginClick = { 
            viewModel.login {
                onLoginClick(uiState.username, uiState.password)
            }
        },
        onRegisterClick = onRegisterClick,
        onForgotPasswordClick = onForgotPasswordClick,
        onGoogleSignInClick = {
            scope.launch {
                signInWithGoogle(context) { idToken ->
                    viewModel.signInWithGoogle(idToken) {
                        onLoginClick(uiState.username, uiState.password)
                    }
                }
            }
        },
        modifier = modifier
    )
}

private suspend fun signInWithGoogle(
    context: Context,
    onSuccess: (String) -> Unit
) {
    try {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.web_client))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            request = request,
            context = context
        )

        val credential = result.credential
        if (credential is androidx.credentials.CustomCredential) {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                onSuccess(idToken)
            }
        }
    } catch (e: GetCredentialException) {
        println("Error en Google Sign-In: ${e.message}")
    } catch (e: Exception) {
        println("Error inesperado: ${e.message}")
    }
}

@Composable
fun LoginScreenContent(
    uiState: LoginUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onGoogleSignInClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    // Colores desde tu paleta MD3 (se ve igual pero ahora responde al tema claro/oscuro)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    GradientBackground {
        androidx.compose.foundation.rememberScrollState().let { scrollState ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Spacer para centrar el contenido cuando hay espacio disponible
                Spacer(modifier = Modifier.weight(1f, fill = false))
                
                AuthCard {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.logoratingroom),
                    contentDescription = stringResource(id = R.string.content_desc_logo),
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Título
                Text(
                    text = stringResource(id = R.string.login_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtítulo
                Text(
                    text = stringResource(id = R.string.login_subtitle),
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Campo de email
                EmailField(
                    value = uiState.username,
                    onValueChange = onEmailChange
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de contraseña
                PasswordField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    showForgotPassword = true,
                    onForgotPasswordClick = onForgotPasswordClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Mensaje de error
                if (uiState.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Botón de iniciar sesión
                CustomButton(
                    text = stringResource(id = R.string.login_button),
                    onClick = onLoginClick,
                    isLoading = uiState.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Divisor "o"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = stringResource(id = R.string.login_or),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de Google Sign-In
                GoogleSignInButton(
                    onClick = onGoogleSignInClick,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Enlace de registro
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.login_no_account),
                        fontSize = 14.sp,
                        color = onSurfaceVariant
                    )
                    TextButton(
                        onClick = onRegisterClick
                    ) {
                        Text(
                            text = stringResource(id = R.string.login_register_link),
                            fontSize = 14.sp,
                            color = primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                    // Texto adicional
                    Text(
                        text = stringResource(id = R.string.login_demo_text),
                        fontSize = 12.sp,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Spacer para centrar el contenido cuando hay espacio disponible
                Spacer(modifier = Modifier.weight(1f, fill = false))
                
                // Spacer mínimo para asegurar padding inferior
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icono de Google (usando el logo de Google desde los recursos)
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.login_with_google),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    RatingRoomTheme {
        LoginScreenContent(
            uiState = LoginUIState(
                username = "",
                password = ""
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onLoginClick = {},
            onRegisterClick = {},
            onForgotPasswordClick = {},
            onGoogleSignInClick = {}
        )
    }
}

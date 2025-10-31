package com.example.ratingroom.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit4.runners.AndroidJUnit4
import com.example.ratingroom.MainActivity
import com.example.ratingroom.data.repository.AuthRepository
import com.example.ratingroom.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RegisterNewUserE2E {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    private lateinit var authRepository: AuthRepository

    @Inject
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        try {
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.firestore.useEmulator(host = "10.0.2.2", port = 8080)
        } catch (e: Exception) {
            // Emulators already configured or not available
        }
        hiltRule.inject()
    }

    @Test
    fun testNewUserRegisterAndLikeReview() {
        // Caso de uso 1: Usuario nuevo con error de contraseña, corrección, navegación y like/unlike
        
        // 1. Usuario nuevo ingresa a la aplicación y va al registro
        composeRule.onNodeWithText("Registrarse").performClick()
        
        // 2. Llena todos los datos con contraseña incorrecta (1234)
        composeRule.onNodeWithText("Nombre").performTextInput("Usuario Test")
        composeRule.onNodeWithText("Email").performTextInput("test@example.com")
        composeRule.onNodeWithText("Contraseña").performTextInput("1234")
        composeRule.onNodeWithText("Confirmar Contraseña").performTextInput("1234")
        
        // 3. Intenta registrarse y verifica mensaje de error
        composeRule.onNodeWithText("Registrar").performClick()
        composeRule.onNodeWithText("La contraseña debe tener al menos 6 caracteres").assertIsDisplayed()
        
        // 4. Corrige la contraseña a 123456
        composeRule.onNodeWithText("Contraseña").performTextClearance()
        composeRule.onNodeWithText("Contraseña").performTextInput("123456")
        composeRule.onNodeWithText("Confirmar Contraseña").performTextClearance()
        composeRule.onNodeWithText("Confirmar Contraseña").performTextInput("123456")
        
        // 5. Se registra exitosamente e ingresa a la aplicación
        composeRule.onNodeWithText("Registrar").performClick()
        composeRule.waitForIdle()
        
        // 6. Verifica que está en el home
        composeRule.onNodeWithText("Películas").assertIsDisplayed()
        
        // 7. Ingresa a la primera película
        composeRule.onAllNodesWithContentDescription("Película").onFirst().performClick()
        composeRule.waitForIdle()
        
        // 8. Verifica que la información de detalle sea correcta
        composeRule.onNodeWithText("Título").assertIsDisplayed()
        composeRule.onNodeWithText("Descripción").assertIsDisplayed()
        composeRule.onNodeWithText("Calificación").assertIsDisplayed()
        
        // 9. Va a la sección de reviews
        composeRule.onNodeWithText("Reviews").performClick()
        composeRule.waitForIdle()
        
        // 10. Da like al primer comentario y verifica que aumenta
        val initialLikes = composeRule.onAllNodesWithContentDescription("Likes count").onFirst()
        val initialLikesText = initialLikes.fetchSemanticsNode().config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.firstOrNull()?.text
        
        composeRule.onAllNodesWithContentDescription("Like button").onFirst().performClick()
        composeRule.waitForIdle()
        
        // Verifica que aumentó el número de likes
        composeRule.onAllNodesWithContentDescription("Likes count").onFirst().assertTextContains((initialLikesText?.toIntOrNull()?.plus(1) ?: 1).toString())
        
        // 11. Va atrás y vuelve a seleccionar la misma película
        composeRule.onNodeWithContentDescription("Atrás").performClick()
        composeRule.onAllNodesWithContentDescription("Película").onFirst().performClick()
        composeRule.onNodeWithText("Reviews").performClick()
        composeRule.waitForIdle()
        
        // 12. Quita el like y verifica que disminuye
        composeRule.onAllNodesWithContentDescription("Like button").onFirst().performClick()
        composeRule.waitForIdle()
        
        // Verifica que disminuyó el número de likes
        composeRule.onAllNodesWithContentDescription("Likes count").onFirst().assertTextContains(initialLikesText ?: "0")
    }

    @Test
    fun testUserLoginAndFollowUser() {
        // Caso de uso 2: Usuario registrado hace login, sigue usuario, verifica publicaciones
        
        // 1. Usuario ya registrado realiza login
        composeRule.onNodeWithText("Iniciar Sesión").performClick()
        composeRule.onNodeWithText("Email").performTextInput("existing@example.com")
        composeRule.onNodeWithText("Contraseña").performTextInput("123456")
        composeRule.onNodeWithText("Ingresar").performClick()
        composeRule.waitForIdle()
        
        // 2. Va al perfil de un usuario
        composeRule.onNodeWithText("Buscar").performClick()
        composeRule.onNodeWithText("Buscar usuarios").performTextInput("usuario_a_seguir")
        composeRule.onAllNodesWithContentDescription("Usuario").onFirst().performClick()
        composeRule.waitForIdle()
        
        // 3. Verifica que la información del usuario sea correcta
        composeRule.onNodeWithText("Nombre de usuario").assertIsDisplayed()
        composeRule.onNodeWithText("Biografía").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Seguidores count").assertIsDisplayed()
        
        // 4. Obtiene el número inicial de seguidores
        val initialFollowers = composeRule.onNodeWithContentDescription("Seguidores count")
        val initialFollowersText = initialFollowers.fetchSemanticsNode().config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)?.firstOrNull()?.text
        
        // 5. Le da follow y verifica que aumenta la cantidad de seguidores
        composeRule.onNodeWithText("Seguir").performClick()
        composeRule.waitForIdle()
        
        // Verifica que aumentó el número de seguidores
        composeRule.onNodeWithContentDescription("Seguidores count").assertTextContains((initialFollowersText?.toIntOrNull()?.plus(1) ?: 1).toString())
        
        // 6. Vuelve al home
        composeRule.onNodeWithContentDescription("Home").performClick()
        composeRule.waitForIdle()
        
        // 7. Va a la sección de publicaciones de seguidos
        composeRule.onNodeWithText("Seguidos").performClick()
        composeRule.waitForIdle()
        
        // 8. Verifica que aparezca al menos una publicación del usuario que acabó de seguir
        composeRule.onNodeWithText("usuario_a_seguir").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Publicación de seguido").assertCountEquals(1, true)
    }
}
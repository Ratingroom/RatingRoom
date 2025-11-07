package com.example.ratingroom.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ratingroom.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegisterNewUserE2E {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun register_new_user_navigates_to_main_menu() {
        // Open Register screen from Login
        composeRule.onNodeWithText("Regístrate aquí").performClick()

        // Fill out registration form
        composeRule.onNode(hasText("Tu nombre") and hasSetTextAction()).performTextInput("Usuario E2E")
        composeRule.onNode(hasText("tu@correo.com") and hasSetTextAction()).performTextInput(randomEmail())
        composeRule.onNode(hasText("Tu contraseña") and hasSetTextAction()).performTextInput("Password123!")
        composeRule.onNode(hasText("Confirma tu contraseña") and hasSetTextAction()).performTextInput("Password123!")
        composeRule.onNode(hasText("Acción, Drama, Comedia...") and hasSetTextAction()).performTextInput("Acción")
        composeRule.onNode(hasText("1990") and hasSetTextAction()).performTextInput("1990")

        // Accept terms and conditions (toggleable Checkbox next to label)
        composeRule.onNode(isToggleable()).performClick()

        // Submit registration
        composeRule.onNodeWithText("Crear Cuenta").performClick()

        // Wait for navigation to Main Menu by asserting search placeholder appears
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodes(hasText("Buscar películas, géneros, directores...")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Buscar películas, géneros, directores...").assertIsDisplayed()
    }

    private fun randomEmail(): String {
        val ts = System.currentTimeMillis()
        return "e2e_$ts@test.local"
    }
}
package com.example.ratingroom.e2e

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.ratingroom.MainActivity
import org.junit.Rule
import org.junit.Test

class FollowUserE2E {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun followUser_and_see_them_in_Following_list() {
        // 1) Login quickly (email + password) and go to Main Menu
        composeRule.onNode(hasText("tu@correo.com") and hasSetTextAction()).performTextInput("user${System.currentTimeMillis()}@mail.com")
        composeRule.onNode(hasText("Tu contraseña") and hasSetTextAction()).performTextInput("Password123!")
        composeRule.onNode(hasText("Iniciar Sesión") and hasClickAction()).performClick()

        // Wait until main menu is visible
        composeRule.waitUntil(timeoutMillis = 15000) {
            composeRule.onAllNodes(hasText("Buscar película, serie o actor"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 2) Open drawer and navigate to Amigos
        composeRule.onNode(hasContentDescription("Logo") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Amigos") and hasClickAction()).performClick()

        // Wait for Friends screen
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasText("Buscar amigos..."))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Go to Descubrir tab (discover new people)
        composeRule.onNode(hasText("Descubrir") and hasClickAction()).performClick()

        // 3) Open a user's profile from Discover and tap Seguir
        // Prefer the explicit content description on the profile icon button
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasContentDescription("Ver perfil"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodes(hasContentDescription("Ver perfil")).onFirst().performClick()

        // Ensure Friend profile screen loaded (top bar back icon present)
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasContentDescription("Atrás"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Tap Seguir and wait for state to toggle to Dejar de seguir
        composeRule.onNode(hasText("Seguir") and hasClickAction()).performClick()
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasText("Dejar de seguir"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // 4) Navigate to Profile -> Siguiendo and verify user appears in the list
        // Back to previous screen (Friends)
        composeRule.onNode(hasContentDescription("Atrás") and hasClickAction()).performClick()

        // Open drawer and go to Mi Perfil
        composeRule.onNode(hasContentDescription("Logo") and hasClickAction()).performClick()
        composeRule.onNode(hasText("Mi Perfil") and hasClickAction()).performClick()

        // Wait for Profile screen content
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasText("Siguiendo"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Click on Siguiendo metric card
        composeRule.onNode(hasText("Siguiendo") and hasClickAction()).performClick()

        // Wait for Following screen title
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasText("Siguiendo"))
                .fetchSemanticsNodes().isNotEmpty()
        }

        // Verify the following list has at least one item (uses the unfollow action icon)
        composeRule.waitUntil(timeoutMillis = 10000) {
            composeRule.onAllNodes(hasContentDescription("Dejar de seguir"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasContentDescription("Dejar de seguir")).assertExists()
    }
}
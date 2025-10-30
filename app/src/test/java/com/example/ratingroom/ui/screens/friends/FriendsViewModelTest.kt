package com.example.ratingroom.ui.screens.friends

import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendshipType
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {

    private val friendsRepository: com.example.ratingroom.repository.FriendsRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `carga lista de amigos y estados derivados`() = runTest(testDispatcher) {
        // Arrange: listas crudas provenientes del repositorio
        coEvery { friendsRepository.getFollowing() } returns listOf(
            Friend(id = 1, uid = "u1", name = "Ana", username = "ana")
        )
        coEvery { friendsRepository.getFollowers() } returns listOf(
            Friend(id = 2, uid = "u1", name = "Ana", username = "ana"),
            Friend(id = 3, uid = "u2", name = "Bob", username = "bob")
        )
        coEvery { friendsRepository.getSuggestions() } returns listOf(
            Friend(id = 4, uid = "u3", name = "Cara", username = "cara")
        )

        // Act
        val vm = FriendsViewModel(friendsRepository)
        // Avanza la ejecución de coroutines de prueba hasta que no haya más tareas pendientes
        advanceUntilIdle()

        // Assert
        val state = vm.uiState.value
        assertThat(state.isLoading).isFalse()
        // Following convertidos a amigos con MUTUAL cuando también están en followers
        assertThat(state.friends).hasSize(1)
        assertThat(state.friends[0].relationshipType).isEqualTo(FriendshipType.MUTUAL)
        assertThat(state.friends[0].isFollowing).isTrue()
        // Followers mantienen orden y marcan follow back cuando son mutuos
        assertThat(state.followers).hasSize(2)
        assertThat(state.followers[0].relationshipType).isEqualTo(FriendshipType.MUTUAL)
        assertThat(state.followers[0].isFollowing).isTrue()
        assertThat(state.followers[1].relationshipType).isEqualTo(FriendshipType.FOLLOWER)
        assertThat(state.followers[1].isFollowing).isFalse()
        // Suggestions filtra uid presentes en following/followers
        assertThat(state.suggestions).hasSize(1)
        assertThat(state.suggestions[0].relationshipType).isEqualTo(FriendshipType.NONE)
        assertThat(state.suggestions[0].isFollowing).isFalse()
    }
}
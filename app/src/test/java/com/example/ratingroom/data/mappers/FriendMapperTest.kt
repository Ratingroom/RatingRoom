package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Friend
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FriendMapperTest {

    @Test
    fun friend_toUi_mapeaTodosLosCampos() {
        val friend = Friend(
            id = 10,
            name = "Laura",
            username = "@laura",
            isOnline = true,
            averageRating = 4.8,
            mutualFriends = 12
        )

        val ui = friend.toUi()
        val expected = FriendUi(10, "Laura", "@laura", true, 4.8, 12)
        assertThat(ui).isEqualTo(expected)
    }

    @Test
    fun friend_toUi_manejaCamposOpcionalesNullOSinValor() {
        val friend = Friend(
            id = 11,
            name = "Pepe",
            username = "@pepe",
            profileImageUrl = null, // opcional
            isOnline = false,
            averageRating = 0.0,
            mutualFriends = 0
        )

        val ui = friend.toUi()
        assertThat(ui.id).isEqualTo(11)
        assertThat(ui.name).isEqualTo("Pepe")
        assertThat(ui.username).isEqualTo("@pepe")
        assertThat(ui.isOnline).isFalse()
        assertThat(ui.averageRating).isEqualTo(0.0)
        assertThat(ui.mutualFriends).isEqualTo(0)
    }

    @Test
    fun listaFriend_toUi_mapeaColeccion() {
        val friends = listOf(
            Friend(id = 1, name = "A", username = "@a", isOnline = false, averageRating = 3.0, mutualFriends = 1),
            Friend(id = 2, name = "B", username = "@b", isOnline = true, averageRating = 5.0, mutualFriends = 0)
        )

        val uiList = friends.toUiList()
        assertThat(uiList).hasSize(2)
        assertThat(uiList[0]).isEqualTo(FriendUi(1, "A", "@a", false, 3.0, 1))
        assertThat(uiList[1]).isEqualTo(FriendUi(2, "B", "@b", true, 5.0, 0))
    }

    @Test
    fun toUi_conValoresBorde() {
        val friend = Friend(
            id = 3,
            name = "Edge",
            username = "@edge",
            isOnline = false,
            averageRating = 5.0,
            mutualFriends = 0
        )
        val ui = friend.toUi()
        assertThat(ui.averageRating).isEqualTo(5.0)
        assertThat(ui.mutualFriends).isEqualTo(0)
    }

    @Test
    fun toUi_noMutatesSource() {
        val friend = Friend(id = 4, name = "X", username = "@x", isOnline = false, averageRating = 1.0, mutualFriends = 2)
        val copyOfSource = friend.copy()
        friend.toUi()
        assertThat(friend).isEqualTo(copyOfSource)
    }
}
package com.example.ratingroom.repository

import com.example.ratingroom.R
import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendActivity

object FriendsRepository {

    private val friendsList = mutableListOf(
        Friend(id = 1, name = "Ana García", username = "ana_garcia", isOnline = true, lastSeen = "Hace 5 horas", isFriend = true, mutualFriends = 5),
        Friend(id = 2, name = "Carlos López", username = "carlos_lopez", isOnline = true, lastSeen = "Hace 3 horas", isFriend = false, mutualFriends = 3),
        Friend(id = 3, name = "María Rodríguez", username = "maria_rodriguez", isOnline = true, lastSeen = "Hace 1 hora", isFriend = true, mutualFriends = 8)
    )

    private val suggestionsList = mutableListOf(
        Friend(id = 4, name = "Pedro Martínez", username = "pedro_martinez", isOnline = true, lastSeen = "Hace 5 horas", isFriend = false, mutualFriends = 2),
        Friend(id = 5, name = "Laura Sánchez", username = "laura_sanchez", isOnline = true, lastSeen = "Hace 5 horas", isFriend = false, mutualFriends = 1),
        Friend(id = 6, name = "Diego Fernández", username = "diego_fernandez", isOnline = true, lastSeen = "Hace 5 horas", isFriend = false, mutualFriends = 4)
    )

    private val followersList = mutableListOf(
        Friend(id = 7, name = "Sofia Morales", username = "sofia_morales", isOnline = true, lastSeen = "Hace 5 horas", isFriend = true, mutualFriends = 0),
        Friend(id = 8, name = "Andrés Ruiz", username = "andres_ruiz", isOnline = true, lastSeen = "Hace 5 horas", isFriend = true, mutualFriends = 1)
    )

    fun getFriends(): List<Friend> {
        return friendsList.toList()
    }

    fun getSuggestions(): List<Friend> {
        return suggestionsList.toList()
    }

    fun getFollowers(): List<Friend> {
        return followersList.toList()
    }

    fun addFriend(friendId: Int) {
        // Buscar en sugerencias y mover a amigos
        val suggestion = suggestionsList.find { it.id == friendId }
        suggestion?.let {
            suggestionsList.remove(it)
            friendsList.add(it.copy(isFriend = true))
        }
    }

    fun removeFriend(friendId: Int) {
        // Buscar en amigos y mover a sugerencias
        val friend = friendsList.find { it.id == friendId }
        friend?.let {
            friendsList.remove(it)
            suggestionsList.add(it.copy(isFriend = false, isFollowing = false))
        }
    }

    fun followUser(userId: Int) {
        // Actualizar estado de seguimiento
        friendsList.find { it.id == userId }?.let { friend ->
            val index = friendsList.indexOf(friend)
            friendsList[index] = friend.copy(isFollowing = true)
        }

        suggestionsList.find { it.id == userId }?.let { suggestion ->
            val index = suggestionsList.indexOf(suggestion)
            suggestionsList[index] = suggestion.copy(isFollowing = true)
        }
    }

    fun unfollowUser(userId: Int) {
        // Actualizar estado de seguimiento
        friendsList.find { it.id == userId }?.let { friend ->
            val index = friendsList.indexOf(friend)
            friendsList[index] = friend.copy(isFollowing = false)
        }

        followersList.find { it.id == userId }?.let { follower ->
            val index = followersList.indexOf(follower)
            followersList[index] = follower.copy(isFollowing = false)
        }
    }

    fun searchFriends(query: String): List<Friend> {
        val allUsers = friendsList + suggestionsList + followersList
        return allUsers.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.username.contains(query, ignoreCase = true)
        }
    }

    fun getFriendsActivity(): List<FriendActivity> {
        return listOf(
            FriendActivity(
                id = 1,
                friend = friendsList[0],
                movie = "The Dark Knight",
                action = "rated",
                rating = 5,
                comment = null,
                timestamp = "Hace 2 horas",
                posterUrl = "https://a.ltrbxd.com/resized/sm/upload/78/y5/zg/ej/oefdD26aey8GPdx7Rm45PNncJdU-0-2000-0-3000-crop.jpg?v=2d0ce4be25"
            ),
            FriendActivity(
                id = 2,
                friend = friendsList[1],
                movie = "Inception",
                action = "reviewed",
                rating = null,
                comment = "Una película increíble",
                timestamp = "Hace 4 horas",
                posterUrl = "https://a.ltrbxd.com/resized/sm/upload/sv/95/s9/4j/inception-0-2000-0-3000-crop.jpg?v=30d7224316"
            ),
            FriendActivity(
                id = 3,
                friend = friendsList[2],
                movie = "Avatar",
                action = "added_to_list",
                rating = null,
                comment = null,
                timestamp = "Hace 6 horas",
                posterUrl = "https://a.ltrbxd.com/resized/sm/upload/1p/mh/li/l2/b7nR3eKeTOwHPKmDLUWunIGasKo-0-2000-0-3000-crop.jpg?v=0bb5ec98ec"
            )
        )
    }
}

package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.UserDto
import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendActivity
import com.example.ratingroom.data.models.FriendshipType
import javax.inject.Inject

class FriendsRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository
) {

    private fun mapUserToFriend(userDto: UserDto): Friend {
        val uid = userDto.uid
        val idHash = uid.hashCode()
        val name = userDto.fullName ?: userDto.displayName ?: "Usuario"
        val username = userDto.username ?: userDto.email?.substringBefore("@") ?: "user"
        val profileImageUrl = userDto.profileImageUrl

        return Friend(
            id = idHash,
            uid = uid,
            name = name,
            username = username,
            profileImageUrl = profileImageUrl,
            isOnline = false,
            lastSeen = "",
            isFriend = false,
            mutualFriends = 0,
            isFollowing = false,
            relationshipType = FriendshipType.NONE
        )
    }

    suspend fun getFollowing(): Result<List<Friend>> {
        return try {
            val currentUid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")
            val maps = firestoreDataSource.getFollowing(currentUid)
            val friends = maps.map { mapUserToFriend(it).copy(isFollowing = true, relationshipType = FriendshipType.FOLLOWING) }
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFollowers(): Result<List<Friend>> {
        return try {
            val currentUid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")
            val maps = firestoreDataSource.getFollowers(currentUid)
            val friends = maps.map { mapUserToFriend(it).copy(relationshipType = FriendshipType.FOLLOWER) }
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSuggestions(): Result<List<Friend>> {
        return try {
            val currentUid = authRepository.currentUser?.uid 
                ?: throw IllegalStateException("Usuario no autenticado")
            val all = firestoreDataSource.getAllUsers()
            val followingUids = firestoreDataSource.getFollowing(currentUid).map { it.uid }.toSet()
            val followersUids = firestoreDataSource.getFollowers(currentUid).map { it.uid }.toSet()
            val suggestions = all.filter { it.uid != currentUid && !(followingUids.contains(it.uid) || followersUids.contains(it.uid)) }
                .map { mapUserToFriend(it) }
            Result.success(suggestions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun followUser(targetUid: String) {
        firestoreDataSource.followUser(targetUid)
    }

    suspend fun unfollowUser(targetUid: String) {
        firestoreDataSource.unfollowUser(targetUid)
    }

    companion object {
        // Mantener API para ActivityTab (por ahora retorna vacío hasta implementar actividad real)
        fun getFriendsActivity(): List<FriendActivity> = emptyList()
    }
}

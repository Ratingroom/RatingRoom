package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.models.Friend
import com.example.ratingroom.data.models.FriendActivity
import com.example.ratingroom.data.models.FriendshipType
import javax.inject.Inject

class FriendsRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val authRepository: AuthRepository
) {

    private fun mapUserToFriend(userMap: Map<String, Any>): Friend {
        val uid = userMap["uid"] as? String ?: ""
        val idHash = uid.hashCode()
        val name = (userMap["fullName"] as? String)
            ?: (userMap["displayName"] as? String)
            ?: "Usuario"
        val username = (userMap["username"] as? String)
            ?: ((userMap["email"] as? String)?.substringBefore("@") ?: "user")
        val profileImageUrl = userMap["profileImageUrl"] as? String

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

    suspend fun getFollowing(): List<Friend> {
        val currentUid = authRepository.currentUser?.uid ?: return emptyList()
        val maps = firestoreDataSource.getFollowing(currentUid)
        return maps.map { mapUserToFriend(it).copy(isFollowing = true, relationshipType = FriendshipType.FOLLOWING) }
    }

    suspend fun getFollowers(): List<Friend> {
        val currentUid = authRepository.currentUser?.uid ?: return emptyList()
        val maps = firestoreDataSource.getFollowers(currentUid)
        return maps.map { mapUserToFriend(it).copy(relationshipType = FriendshipType.FOLLOWER) }
    }

    suspend fun getSuggestions(): List<Friend> {
        val currentUid = authRepository.currentUser?.uid ?: return emptyList()
        val all = firestoreDataSource.getAllUsers()
        val followingUids = firestoreDataSource.getFollowing(currentUid).map { it["uid"] as? String }.toSet()
        val followersUids = firestoreDataSource.getFollowers(currentUid).map { it["uid"] as? String }.toSet()
        return all.filter { (it["uid"] as? String) != currentUid && !(followingUids.contains(it["uid"] as? String) || followersUids.contains(it["uid"] as? String)) }
            .map { mapUserToFriend(it) }
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

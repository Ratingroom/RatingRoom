package com.example.ratingroom.data.models

data class Friend(
    val id: Int,
    val uid: String? = null, // UID de Firebase del usuario
    val name: String,
    val username: String,
    val profileImageUrl: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: String = "",
    val isFriend: Boolean = false,
    val mutualFriends: Int = 0,
    val isFollowing: Boolean = false,
    val relationshipType: FriendshipType = FriendshipType.NONE,
    val favoriteGenres: List<String> = emptyList(),
    val totalReviews: Int = 0,
    val averageRating: Double = 0.0

)

enum class FriendshipType {
    FRIEND,
    FOLLOWING,
    FOLLOWER,
    MUTUAL,
    NONE
}

data class FriendRequest(
    val id: Int,
    val fromUserId: Int,
    val toUserId: Int,
    val status: RequestStatus,
    val timestamp: String
)

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
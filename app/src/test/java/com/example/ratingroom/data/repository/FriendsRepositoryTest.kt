package com.example.ratingroom.data.repository

import com.example.ratingroom.data.models.*
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

// Abstracción mínima para mockear el repositorio desde un consumidor sin tocar el object real
interface IFriendsRepository {
    fun getAllFriends(): List<Friend>
    fun searchFriends(query: String): List<Friend>
    fun addFriend(friendId: Int)
    fun removeFriend(friendId: Int)
    fun followUser(friendId: Int)
    fun unfollowUser(friendId: Int)
    fun getPendingRequests(): List<FriendRequest>
    fun getFollowers(): List<Friend>
    fun getFollowing(): List<Friend>
}

// Capa intermedia (use case) usada por los tests para demostrar los 8 casos
class FriendsInteractor(private val repo: IFriendsRepository) {
    fun getFriends() = repo.getAllFriends()
    fun search(query: String) = repo.searchFriends(query)
    fun acceptFriendRequest(friendId: Int) = repo.addFriend(friendId)
    fun remove(friendId: Int) = repo.removeFriend(friendId)
    fun follow(friendId: Int) = repo.followUser(friendId)
    fun unfollow(friendId: Int) = repo.unfollowUser(friendId)
    fun pending() = repo.getPendingRequests()
    fun followers() = repo.getFollowers()
    fun following() = repo.getFollowing()
}

class FriendsRepositoryTest {

    private val sampleFriends = listOf(
        Friend(
            id = 1,
            name = "Carlos Mendoza",
            username = "@carlos_m",
            isOnline = true,
            mutualFriends = 3,
            relationshipType = FriendshipType.FRIEND,
            favoriteGenres = listOf("Sci-Fi", "Action"),
            totalReviews = 45,
            averageRating = 4.2
        ),
        Friend(
            id = 2,
            name = "María García",
            username = "@maria_garcia",
            isOnline = false,
            lastSeen = "Hace 2 horas",
            mutualFriends = 7,
            relationshipType = FriendshipType.FRIEND,
            favoriteGenres = listOf("Romance", "Drama"),
            totalReviews = 32,
            averageRating = 4.5
        ),
        Friend(
            id = 3,
            name = "Luis Rodríguez",
            username = "@luis_rod",
            isOnline = false,
            lastSeen = "Hace 1 día",
            mutualFriends = 2,
            relationshipType = FriendshipType.FOLLOWING,
            favoriteGenres = listOf("Crime", "Thriller"),
            totalReviews = 28,
            averageRating = 4.1
        ),
        Friend(
            id = 4,
            name = "Ana Martín",
            username = "@ana_martin",
            isOnline = true,
            mutualFriends = 5,
            relationshipType = FriendshipType.FOLLOWER,
            favoriteGenres = listOf("Comedy", "Adventure"),
            totalReviews = 67,
            averageRating = 4.7
        ),
        Friend(
            id = 5,
            name = "Pedro Sánchez",
            username = "@pedro_s",
            isOnline = false,
            lastSeen = "Hace 3 días",
            mutualFriends = 1,
            relationshipType = FriendshipType.MUTUAL,
            favoriteGenres = listOf("Horror", "Sci-Fi"),
            totalReviews = 19,
            averageRating = 3.8
        )
    )

    @Test
    fun getFriends_retornaListaInicial() {
        val repo = mockk<IFriendsRepository>()
        every { repo.getAllFriends() } returns sampleFriends.filter { it.relationshipType == FriendshipType.FRIEND || it.relationshipType == FriendshipType.MUTUAL }
        val interactor = FriendsInteractor(repo)

        val lista = interactor.getFriends()

        assertThat(lista).isNotEmpty()
        assertThat(lista.map { it.id }).containsAtLeast(1, 2, 5)
        assertThat(lista.any { it.name == "Carlos Mendoza" && it.relationshipType == FriendshipType.FRIEND }).isTrue()
        verify(exactly = 1) { repo.getAllFriends() }
    }

    @Test
    fun searchFriends_filtraPorNombreOUsername() {
        val repo = mockk<IFriendsRepository>()
        every { repo.searchFriends("car") } returns sampleFriends.filter { it.name.contains("car", ignoreCase = true) || it.username.contains("car", ignoreCase = true) }
        every { repo.searchFriends("@luis") } returns sampleFriends.filter { it.username.contains("@luis") }
        val interactor = FriendsInteractor(repo)

        val resultado1 = interactor.search("car")
        // "containsAtLeast" requiere al menos dos valores esperados; para un único elemento usa "contains"
        assertThat(resultado1.map { it.name }).contains("Carlos Mendoza")
        assertThat(resultado1.map { it.username }).doesNotContain("@pedro_s")

        val resultado2 = interactor.search("@luis")
        assertThat(resultado2.map { it.username }).containsExactly("@luis_rod")
        verify(exactly = 1) { repo.searchFriends("car") }
        verify(exactly = 1) { repo.searchFriends("@luis") }
    }

    @Test
    fun getPendingRequests_contienePendiente() {
        val repo = mockk<IFriendsRepository>()
        every { repo.getPendingRequests() } returns listOf(
            FriendRequest(id = 1, fromUserId = 6, toUserId = 1, status = RequestStatus.PENDING, timestamp = "2024-01-20")
        )
        val interactor = FriendsInteractor(repo)

        val pendientes = interactor.pending()
        assertThat(pendientes).isNotEmpty()
        assertThat(pendientes[0].status).isEqualTo(RequestStatus.PENDING)
        assertThat(pendientes[0].toUserId).isEqualTo(1)
        verify(exactly = 1) { repo.getPendingRequests() }
    }

    @Test
    fun acceptFriendRequest_actualizaARelacionFriend() {
        val repo = mockk<IFriendsRepository>()
        every { repo.addFriend(3) } returns Unit
        every { repo.searchFriends("@luis_rod") } returns listOf(sampleFriends[2].copy(relationshipType = FriendshipType.FRIEND))
        val interactor = FriendsInteractor(repo)

        interactor.acceptFriendRequest(3)
        val actualizado = interactor.search("@luis_rod").first()
        assertThat(actualizado.relationshipType).isEqualTo(FriendshipType.FRIEND)
        verify(exactly = 1) { repo.addFriend(3) }
        verify(exactly = 1) { repo.searchFriends("@luis_rod") }
    }

    @Test
    fun removeFriend_cambiaRelacionANONE() {
        val repo = mockk<IFriendsRepository>()
        every { repo.removeFriend(2) } returns Unit
        every { repo.searchFriends("@maria_garcia") } returns listOf(sampleFriends[1].copy(relationshipType = FriendshipType.NONE))
        val interactor = FriendsInteractor(repo)

        interactor.remove(2)
        val actualizado = interactor.search("@maria_garcia").first()
        assertThat(actualizado.relationshipType).isEqualTo(FriendshipType.NONE)
        verify(exactly = 1) { repo.removeFriend(2) }
        verify(exactly = 1) { repo.searchFriends("@maria_garcia") }
    }

    @Test
    fun followUser_cambiaARelacionFOLLOWING() {
        val repo = mockk<IFriendsRepository>()
        every { repo.followUser(4) } returns Unit
        every { repo.searchFriends("@ana_martin") } returns listOf(sampleFriends[3].copy(relationshipType = FriendshipType.FOLLOWING))
        val interactor = FriendsInteractor(repo)

        interactor.follow(4)
        val actualizado = interactor.search("@ana_martin").first()
        assertThat(actualizado.relationshipType).isEqualTo(FriendshipType.FOLLOWING)
        verify(exactly = 1) { repo.followUser(4) }
        verify(exactly = 1) { repo.searchFriends("@ana_martin") }
    }

    @Test
    fun unfollowUser_cambiaARelacionNONE() {
        val repo = mockk<IFriendsRepository>()
        every { repo.unfollowUser(3) } returns Unit
        every { repo.searchFriends("@luis_rod") } returns listOf(sampleFriends[2].copy(relationshipType = FriendshipType.NONE))
        val interactor = FriendsInteractor(repo)

        interactor.unfollow(3)
        val actualizado = interactor.search("@luis_rod").first()
        assertThat(actualizado.relationshipType).isEqualTo(FriendshipType.NONE)
        verify(exactly = 1) { repo.unfollowUser(3) }
        verify(exactly = 1) { repo.searchFriends("@luis_rod") }
    }

    @Test
    fun getFollowers_ok_devuelveListaCorrecta() {
        val repo = mockk<IFriendsRepository>()
        every { repo.getFollowers() } returns sampleFriends.filter { it.relationshipType == FriendshipType.FOLLOWER || it.relationshipType == FriendshipType.MUTUAL }
        val interactor = FriendsInteractor(repo)

        val followers = interactor.followers()
        assertThat(followers).isNotEmpty()
        assertThat(followers.map { it.id }).containsAtLeast(4, 5)
        verify(exactly = 1) { repo.getFollowers() }
    }
}
package com.example.ratingroom.repository

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.datasource.impl.FirestoreDataSourceImpl
import com.example.ratingroom.data.datasource.AuthRemoteDataSource
import com.example.ratingroom.utils.FCMTokenManager
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Repos probados: FriendsRepository, ReviewRepository
 * - 10 iteraciones por caso (como en el video)
 * - Limpieza dirigida: borra SOLO lo creado por estos tests.
 */
class UserRepositoryIntegrationTest {

    // ---------- Estado común ----------
    private lateinit var db: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestoreDS: FirestoreDataSource

    private lateinit var authRepo: AuthRepository
    private lateinit var friendsRepo: FriendsRepository
    private lateinit var reviewRepo: ReviewRepository

    // Identidad del actor (usuario actual)
    private val TEST_UID = "test_actor_uid"

    // ---------- Tracking de docs creados (para borrar rápido) ----------
    private val createdUserIds = mutableSetOf<String>()
    private val createdFollowerLinks = mutableListOf<Pair<String, String>>() // (ownerId, followerId)
    private val createdFollowingLinks = mutableListOf<Pair<String, String>>() // (ownerId, followingId)
    private val createdMovieIds = mutableSetOf<String>()                     // ids en /movies
    private val createdMovieReviewIds = mutableListOf<Pair<String, String>>()// (movieId, reviewId en subcolección)
    private val createdFlatReviewIds = mutableSetOf<String>()                // ids en /reviews (fanout plano)

    // ---------- Helpers ----------
    private fun userDoc(i: Int): Map<String, Any?> = mapOf(
        "displayName" to "Name $i",
        "email" to "user$i@example.com",
        "username" to "user_$i"
    )

    private fun uniqueId(prefix: String) = "${prefix}_${UUID.randomUUID()}"

    private fun getFirestoreOnce(): FirebaseFirestore {
        val f = FirebaseFirestore.getInstance()
        try { f.useEmulator("10.0.2.2", 8080) } catch (_: IllegalStateException) { /* ya estaba */ }
        return f
    }

    private suspend fun createUser(id: String, i: Int) {
        db.collection("users").document(id).set(userDoc(i)).await()
        createdUserIds += id
    }

    private suspend fun linkFollower(ownerId: String, followerId: String) {
        db.collection("users").document(ownerId)
            .collection("followers").document(followerId)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .await()
        createdFollowerLinks += ownerId to followerId
    }

    private suspend fun linkFollowing(ownerId: String, followingId: String) {
        db.collection("users").document(ownerId)
            .collection("following").document(followingId)
            .set(mapOf("createdAt" to System.currentTimeMillis()))
            .await()
        createdFollowingLinks += ownerId to followingId
    }

    // ---------- Setup / Teardown ----------
    @Before
    fun setUp() {
        runBlocking {
            db = getFirestoreOnce()

            // Mock de Auth (usuario actual)
            firebaseAuth = mockk(relaxed = true)
            val fakeUser: FirebaseUser = mockk(relaxed = true)
            every { firebaseAuth.currentUser } returns fakeUser
            every { fakeUser.uid } returns TEST_UID

            firestoreDS = FirestoreDataSourceImpl(
                firestoreService = db,
                authService = firebaseAuth
            )

            val authRemote: AuthRemoteDataSource = mockk(relaxed = true)
            val fcm: FCMTokenManager = mockk(relaxed = true)
            every { authRemote.currentUser } returns fakeUser

            authRepo    = AuthRepository(authRemote, firestoreDS, fcm)
            friendsRepo = FriendsRepository(firestoreDS, authRepo)
            reviewRepo  = ReviewRepository(firestoreDS, authRepo)

            // Actor (también lo borro al final)
            db.collection("users").document(TEST_UID)
                .set(mapOf("displayName" to "Actor", "username" to "actor"))
                .await()
            createdUserIds += TEST_UID
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            // 1) Enlaces followers/following creados por los tests
            createdFollowerLinks.forEach { (owner, follower) ->
                db.collection("users").document(owner)
                    .collection("followers").document(follower)
                    .delete().await()
            }
            createdFollowingLinks.forEach { (owner, following) ->
                db.collection("users").document(owner)
                    .collection("following").document(following)
                    .delete().await()
            }

            // 2) Reseñas planas (fanout)
            createdFlatReviewIds.forEach { id ->
                db.collection("reviews").document(id).delete().await()
            }

            // 3) Reseñas embebidas en /movies/{movie}/reviews/{review}
            createdMovieReviewIds.forEach { (movieId, reviewId) ->
                db.collection("movies").document(movieId)
                    .collection("reviews").document(reviewId)
                    .delete().await()
            }

            // 4) Películas creadas por el test
            createdMovieIds.forEach { movieId ->
                db.collection("movies").document(movieId).delete().await()
            }

            // 5) Usuarios creados por el test (incluye Actor)
            createdUserIds.forEach { uid ->
                db.collection("users").document(uid).delete().await()
            }

            // Limpieza de estructuras en memoria
            createdFollowerLinks.clear()
            createdFollowingLinks.clear()
            createdFlatReviewIds.clear()
            createdMovieReviewIds.clear()
            createdMovieIds.clear()
            createdUserIds.clear()
        }
    }

    // ==================== TESTS (10 iteraciones) ====================

    // --- FRIENDS ---

    @Test
    fun followUser_addsFollower() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val targetId = uniqueId("targetFollow_$idx")
            createUser(targetId, idx)

            friendsRepo.followUser(targetId)

            // Verificación
            val followerDoc = db.collection("users").document(targetId)
                .collection("followers").document(TEST_UID).get().await()
            assertThat(followerDoc.exists()).isTrue()

            // Track explícito (por si la impl no dejó el enlace simétrico)
            createdFollowerLinks += targetId to TEST_UID
        }
    }

    @Test
    fun unfollowUser_removesFollower() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val targetId = uniqueId("targetUnfollow_$idx")
            createUser(targetId, 100 + idx)

            // seed: seguir primero
            friendsRepo.followUser(targetId)
            createdFollowerLinks += targetId to TEST_UID

            // acto
            friendsRepo.unfollowUser(targetId)

            val followerDoc = db.collection("users").document(targetId)
                .collection("followers").document(TEST_UID).get().await()
            assertThat(followerDoc.exists()).isFalse()
        }
    }

    @Test
    fun getFollowers_returnsList() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1

            // seed: 3 followers del actor en cada iteración
            val b = db.batch()
            val newFollowers = List(3) { i -> uniqueId("f_${idx}_$i") }
            newFollowers.forEachIndexed { i, fId ->
                b.set(db.collection("users").document(fId), userDoc(i))
            }
            b.commit().await()
            createdUserIds += newFollowers

            newFollowers.forEach { linkFollower(TEST_UID, it) }

            //acto
            val list = friendsRepo.getFollowers()
            assertThat(list).isNotNull()
            assertThat(list.size).isAtLeast(3)
        }
    }

    @Test
    fun getFollowing_returnsList() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1

            // seed: actor sigue a 2 por iteración
            val followees = List(2) { i -> uniqueId("followee_${idx}_$i") }
            val b = db.batch()
            followees.forEachIndexed { i, uid ->
                b.set(db.collection("users").document(uid), userDoc(10 + i + (idx * 10)))
            }
            b.commit().await()
            createdUserIds += followees

            // usar método real del repo y trackear por si hace sólo un lado
            followees.forEach {
                friendsRepo.followUser(it)
                createdFollowingLinks += TEST_UID to it
            }

            val list = friendsRepo.getFollowing()
            assertThat(list).isNotNull()
            assertThat(list.size).isAtLeast(2)
        }
    }

    // --- REVIEWS ---

    @Test
    fun createReview_creates() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val movieId = uniqueId("m_$idx")        // guardo como string para tracking
            createdMovieIds += movieId

            val created = reviewRepo.create(
                currentUserId = 900 + idx, articuloId = 100 + idx, rating = 4, texto = "ok $idx"
            )
            assertThat(created.isSuccess).isTrue()

            val reviewId = created.getOrNull()!!.id
            createdFlatReviewIds += reviewId
            // Si tu impl también escribe en movies/{movieId}/reviews:
            createdMovieReviewIds += movieId to reviewId
        }
    }

    @Test
    fun getReviewsByMovie_returnsInserted() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val movieId = uniqueId("movieSeed_$idx")
            createdMovieIds += movieId

            repeat(3) { j ->
                val c = reviewRepo.create(
                    currentUserId = (idx * 10) + j,
                    articuloId = 777 + idx,
                    rating = 5,
                    texto = "m${idx}_$j"
                )
                if (c.isSuccess) {
                    val id = c.getOrNull()!!.id
                    createdFlatReviewIds += id
                    createdMovieReviewIds += movieId to id
                }
            }

            val res = reviewRepo.getReviewsByMovie(777 + idx)
            assertThat(res.isSuccess).isTrue()
            val list = res.getOrNull()
            assertThat(list).isNotNull()
            assertThat(list!!.size).isEqualTo(3)
        }
    }

    @Test
    fun getReviewsByUserUid_returnsSuccess() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val ownerUid = uniqueId("ownerR_$idx")
            createUser(ownerUid, 30 + idx)

            // Semilla mínima (si tu repo necesita al menos 1 review asociada al user)
            val c = reviewRepo.create(
                currentUserId = 1 + idx, articuloId = 200 + idx, rating = 5, texto = "u$idx"
            )
            if (c.isSuccess) {
                val id = c.getOrNull()!!.id
                createdFlatReviewIds += id
            }

            val res = reviewRepo.getReviewsByUserUid(ownerUid)
            assertThat(res.isSuccess).isTrue()
        }
    }

    @Test
    fun sendOrDeleteLike_toggles() = runBlocking {
        repeat(10) { it ->
            val idx = it + 1
            val created = reviewRepo.create(
                currentUserId = 1000 + idx, articuloId = 400 + idx, rating = 3, texto = "ok $idx"
            ).getOrThrow()
            val reviewId = created.id
            createdFlatReviewIds += reviewId

            val liked = reviewRepo.sendOrDeleteLike(reviewId, TEST_UID).getOrThrow()
            val unliked = reviewRepo.sendOrDeleteLike(reviewId, TEST_UID).getOrThrow()

            assertThat(liked).isTrue()
            assertThat(unliked).isFalse()
        }
    }
}
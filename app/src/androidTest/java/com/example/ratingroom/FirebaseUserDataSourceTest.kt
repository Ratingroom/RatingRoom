package com.example.ratingroom

import com.example.ratingroom.data.datasource.impl.FirestoreDataSourceImpl
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

class FirebaseUserDataSourceTest {

    // -------- estado por prueba --------
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var dataSource: FirestoreDataSourceImpl
    private val TEST_UID = "test_actor_uid"

    // -------- helpers --------
    private fun userDoc(i: Int): Map<String, Any?> = mapOf(
        "displayName" to "Name $i",
        "fullName" to "Name $i",
        "email" to "user$i@example.com",
        "username" to "user_$i",
        "biography" to "Bio de usuario $i",
        "location" to "Colombia",
        "favoriteGenre" to "Action",
        "birthYear" to "199$i",
        "birthdate" to "199$i-01-01",
        "website" to "https://example.com/$i",
        "profileImageUrl" to "https://picsum.photos/200/200?random=$i",
        "coverImageUrl" to "https://picsum.photos/800/200?random=$i",
        "followersCount" to i * 10,
        "followingCount" to i * 5,
        "verified" to (i % 2 == 0),
        "createdAt" to "2025-01-01T08:00:00Z",
        "updatedAt" to "2025-01-01T08:00:00Z"
    )

    private fun uniqueId(prefix: String) = "${prefix}_${UUID.randomUUID()}"

    /** Configura el emulador SOLO la 1ª vez; luego reusa la instancia */
    private fun getFirestore(): FirebaseFirestore {
        val instance = FirebaseFirestore.getInstance()
        try { instance.useEmulator("10.0.2.2", 8080) } catch (_: IllegalStateException) { /* ya estaba */ }
        return instance
    }

    // ================= BEFORE / AFTER =================
    @Before
    fun setUp() {
        runBlocking {
            db = getFirestore()

            // Auth mock (sin emulador de Auth)
            auth = mockk(relaxed = true)
            val fakeUser = mockk<FirebaseUser>(relaxed = true)
            every { auth.currentUser } returns fakeUser
            every { fakeUser.uid } returns TEST_UID

            dataSource = FirestoreDataSourceImpl(db, auth)

            // actor
            db.collection("users").document(TEST_UID).set(
                mapOf("displayName" to "Actor", "email" to "actor@example.com", "username" to "actor")
            ).await()

            // seed 10 users
            val batch = db.batch()
            repeat(10) { i ->
                batch.set(db.collection("users").document("user_$i"), userDoc(i))
            }
            batch.commit().await()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            // users + subcolecciones
            val users = db.collection("users").get().await()
            for (u in users.documents) {
                u.reference.collection("followers").get().await().documents.forEach { it.reference.delete().await() }
                u.reference.collection("following").get().await().documents.forEach { it.reference.delete().await() }
                u.reference.collection("notifications").get().await().documents.forEach { it.reference.delete().await() }
                u.reference.delete().await()
            }
            // movies + reviews embebidas
            val movies = db.collection("movies").get().await()
            for (m in movies.documents) {
                m.reference.collection("reviews").get().await().documents.forEach { it.reference.delete().await() }
                m.reference.delete().await()
            }
            // reviews planas (si tu impl las usa)
            db.collection("reviews").get().await().documents.forEach { it.reference.delete().await() }
        }
    }

    // ================= PRUEBAS (8) — 10 iteraciones c/u =================

    /** 1) getUserProfileById: 10 usuarios únicos → get & assert en cada iteración */
    @Test
    fun getUserProfileById_returnsCorrectUser_in10Iterations() {
        runBlocking {
            repeat(10) { i ->
                val uid = uniqueId("user_get")
                db.collection("users").document(uid).set(userDoc(3)).await()
                val u = dataSource.getUserProfileById(uid)
                assertThat(u?.displayName).isEqualTo("Name 3")
                assertThat(u?.username).isEqualTo("user_3")
            }
        }
    }

    /** 2) getAllUsers: consultar 10 veces; siempre debe ser >= 10 (por el seed) */
    @Test
    fun getAllUsers_returnsAtLeast10_in10Iterations() {
        runBlocking {
            repeat(10) {
                val all = dataSource.getAllUsers()
                assertThat(all.size).isAtLeast(10)
            }
        }
    }

    /** 3) followUser: 10 targets nuevos; actor debe quedar en followers en cada uno */
    @Test
    fun followUser_addsCurrentToTargetsFollowers_in10Iterations() {
        runBlocking {
            repeat(10) {
                val targetId = uniqueId("targetFollow")
                db.collection("users").document(targetId).set(userDoc(1)).await()

                val ok = dataSource.followUser(targetId)
                assertThat(ok).isTrue()

                val followers = dataSource.getFollowers(targetId)
                assertThat(followers.any { it.uid == TEST_UID }).isTrue()
            }
        }
    }

    /** 4) unfollowUser: 10 targets → follow + unfollow y comprobar que se elimine */
    @Test
    fun unfollowUser_removesCurrentFromTargetsFollowers_in10Iterations() {
        runBlocking {
            repeat(10) {
                val targetId = uniqueId("targetUnfollow")
                db.collection("users").document(targetId).set(userDoc(2)).await()
                dataSource.followUser(targetId)

                val ok = dataSource.unfollowUser(targetId)
                assertThat(ok).isTrue()

                val followers = dataSource.getFollowers(targetId)
                assertThat(followers.any { it.uid == TEST_UID }).isFalse()
            }
        }
    }

    /** 5) createReviewFanout: 10 reseñas (owners únicos) deben existir en colección plana */
    @Test
    fun createReviewFanout_createsReviewDocument_in10Iterations() {
        runBlocking {
            repeat(10) {
                val owner = uniqueId("reviewOwner")
                db.collection("users").document(owner).set(userDoc(4)).await()

                val reviewId = dataSource.createReviewFanout(
                    userId = owner, movieId = 100, rating = 4, text = "Muy buena"
                )
                val exists = db.collection("reviews").document(reviewId).get().await().exists()
                assertThat(exists).isTrue()
            }
        }
    }

    /** 6) getReviewsByMovie: 10 películas únicas con 5 reseñas c/u → validar tamaño */
    @Test
    fun getReviewsByMovie_returnsInsertedOnes_in10Iterations() {
        runBlocking {
            repeat(10) {
                val movieId = (1000..9999).random()
                repeat(5) { i ->
                    val u = uniqueId("uMovie")
                    db.collection("users").document(u).set(userDoc(i)).await()
                    dataSource.createReviewFanout(u, movieId, (i % 5) + 1, "r$i")
                }
                val list = dataSource.getReviewsByMovie(movieId)
                assertThat(list.size).isEqualTo(5)
                assertThat(list.map { it.pelicula_id }.toSet()).containsExactly(movieId)
            }
        }
    }

    /** 7) getReviewsByUser: 10 users distintos con 3 reseñas c/u → validar tamaño */
    @Test
    fun getReviewsByUser_returnsInsertedOnes_in10Iterations() {
        runBlocking {
            repeat(10) {
                val userId = uniqueId("userReviews")
                db.collection("users").document(userId).set(userDoc(7)).await()
                repeat(3) { i ->
                    dataSource.createReviewFanout(userId, 300 + i, 5, "u7-$i")
                }
                val list = dataSource.getReviewsByUser(userId)
                assertThat(list.size).isEqualTo(3)
            }
        }
    }

    /** 8) sendOrDeleteLike: 10 reseñas → like (true) y unlike (false) en cada una */
    @Test
    fun sendOrDeleteLike_togglesLikeOnReview_in10Iterations() {
        runBlocking {
            repeat(10) {
                val owner = uniqueId("likeOwner")
                db.collection("users").document(owner).set(userDoc(8)).await()
                val reviewId = dataSource.createReviewFanout(owner, 400, 3, "ok")

                val liked = dataSource.sendOrDeleteLike(reviewId, TEST_UID)
                val unliked = dataSource.sendOrDeleteLike(reviewId, TEST_UID)

                assertThat(liked).isTrue()
                assertThat(unliked).isFalse()
            }
        }
    }
}

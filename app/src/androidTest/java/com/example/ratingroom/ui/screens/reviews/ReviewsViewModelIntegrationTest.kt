package com.example.ratingroom.ui.screens.reviews

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.ratingroom.repository.ReviewRepository
import com.example.ratingroom.repository.MovieRepository
import com.example.ratingroom.repository.AuthRepository

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReviewsViewModelIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var reviewRepository: ReviewRepository
    @Inject lateinit var movieRepository: MovieRepository
    @Inject lateinit var authRepository: AuthRepository

    private val reviewId = "r1"
    private val movieId = 101

    @Before
    fun setup() = runBlocking {
        hiltRule.inject()
        // Sign in an anonymous user if needed
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        // Ensure clean state for this review
        cleanupSeed()
        seedData(likes = 5)
    }

    @After
    fun tearDown() = runBlocking {
        cleanupSeed()
    }

    @Test
    fun loadReviews_populatesUiWithFirestoreData() = runBlocking {
        val vm = ReviewsViewModel(reviewRepository, movieRepository, authRepository)

        val state = awaitReviews(vm)
        assertThat(state.reviews).isNotEmpty()
        val first = state.reviews.first()
        assertThat(first.id).isEqualTo(reviewId)
        assertThat(first.movieId).isEqualTo(movieId)
        assertThat(first.movieTitle).isEqualTo("Mi película")
        assertThat(first.likes).isEqualTo(5)
        assertThat(first.isLiked).isFalse()
    }

    @Test
    fun sendOrDeleteLike_togglesLikeAndUpdatesUiAndFirestore() = runBlocking {
        val vm = ReviewsViewModel(reviewRepository, movieRepository, authRepository)

        // Wait initial load
        awaitReviews(vm)

        val currentUid = auth.currentUser!!.uid
        vm.sendOrDeleteLike(reviewId, currentUid)
        // allow async update to propagate
        val likedState = awaitReviews(vm)
        val likedFirst = likedState.reviews.first { it.id == reviewId }
        assertThat(likedFirst.likes).isEqualTo(6)
        assertThat(likedFirst.isLiked).isTrue()

        // Verify Firestore likes count updated
        val rootDoc1 = firestore.collection("reviews").document(reviewId).get().await()
        val likesAfterLike = (rootDoc1.data?.get("likes") as? Number)?.toInt() ?: 0
        assertThat(likesAfterLike).isEqualTo(6)

        // Toggle again: unlike
        vm.sendOrDeleteLike(reviewId, currentUid)
        val unlikedState = awaitReviews(vm)
        val unlikedFirst = unlikedState.reviews.first { it.id == reviewId }
        assertThat(unlikedFirst.likes).isEqualTo(5)
        assertThat(unlikedFirst.isLiked).isFalse()

        val rootDoc2 = firestore.collection("reviews").document(reviewId).get().await()
        val likesAfterUnlike = (rootDoc2.data?.get("likes") as? Number)?.toInt() ?: 0
        assertThat(likesAfterUnlike).isEqualTo(5)
    }

    // Helpers
    private suspend fun seedData(likes: Int) {
        val uid = auth.currentUser!!.uid
        val movieData = mapOf(
            "id" to movieId,
            "title" to "Mi película",
            "year" to "2020",
            "genre" to "Drama",
            "rating" to 4.5,
            "reviews" to 100,
            "description" to "Desc",
            "director" to "Dir",
            "duration" to "120m",
            "imageUrl" to null
        )
        firestore.collection("movies").document(movieId.toString()).set(movieData).await()

        val reviewData = mapOf(
            "id" to reviewId,
            "movieId" to movieId,
            "userId" to uid,
            "rating" to 4,
            "text" to "Muy buena",
            "likes" to likes
        )
        // Root review doc
        firestore.collection("reviews").document(reviewId).set(reviewData).await()
        // Fanout to movie
        firestore.collection("movies").document(movieId.toString())
            .collection("reviews").document(reviewId).set(reviewData).await()
        // Fanout to user
        firestore.collection("users").document(uid)
            .collection("reviews").document(reviewId).set(reviewData).await()
    }

    private suspend fun cleanupSeed() {
        val uid = auth.currentUser?.uid
        firestore.collection("reviews").document(reviewId).delete().await()
        firestore.collection("movies").document(movieId.toString())
            .collection("reviews").document(reviewId).delete().await()
        if (uid != null) {
            firestore.collection("users").document(uid)
                .collection("reviews").document(reviewId).delete().await()
        }
        // Movie document may remain; keep it consistent for tests
    }

    private suspend fun awaitReviews(vm: ReviewsViewModel, timeoutMs: Long = 5000): ReviewsUIState {
        val start = System.currentTimeMillis()
        var last = vm.uiState.value
        while (System.currentTimeMillis() - start < timeoutMs) {
            val state = vm.uiState.value
            last = state
            if (!state.isLoading && state.reviews.isNotEmpty()) {
                return state
            }
            delay(100)
        }
        return last
    }
}
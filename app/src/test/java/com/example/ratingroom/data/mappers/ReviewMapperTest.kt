package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Review
import com.google.common.truth.Truth.assertThat
import org.junit.Test


class ReviewMapperTest {

    @Test
    fun review_toUi_mapeaTodosLosCampos() {
        val review = Review(
            id = "r1",
            movieId = 10,
            userId = 100,
            rating = 4.5,
            comment = "Excelente",
            date = "2024-01-01",
            userName = "Jorge",
            userImageUrl = "https://img/user.jpg",
            likes = 50,
            isLiked = true
        )
        val ui = review.toUi()
        val expected = ReviewUi(
            id = "r1",
            movieId = 10,
            userId = 100,
            rating = 4.5,
            comment = "Excelente",
            date = "2024-01-01",
            userName = "Jorge",
            userImageUrl = "https://img/user.jpg",
            likes = 50,
            isLiked = true
        )
        assertThat(ui).isEqualTo(expected)
    }

    @Test
    fun review_toUi_manejaOpcionalesNull() {
        val review = Review(
            id = "r2",
            movieId = 20,
            userId = 200,
            rating = 3.0,
            comment = "Bien",
            date = "2024-02-02",
            userName = null,
            userImageUrl = null,
            likes = 0,
            isLiked = false
        )
        val ui = review.toUi()
        assertThat(ui.userName).isNull()
        assertThat(ui.userImageUrl).isNull()
    }

    @Test
    fun listaReview_toUi_mapeaColeccion() {
        val reviews = listOf(
            Review("a", 1, 1, 5.0, "", "", null, null, 0, false),
            Review("b", 2, 2, 1.0, "", "", "U", "img", 10, true)
        )
        val uiList = reviews.toUiList()
        assertThat(uiList).hasSize(2)
        assertThat(uiList[0]).isEqualTo(ReviewUi("a", 1, 1, 5.0, "", "", null, null, 0, false))
        assertThat(uiList[1]).isEqualTo(ReviewUi("b", 2, 2, 1.0, "", "", "U", "img", 10, true))
    }

    @Test
    fun toUi_conValoresBorde() {
        val review = Review("edge", 0, 0, 0.0, "", "", null, null, 0, false)
        val ui = review.toUi()
        assertThat(ui.rating).isEqualTo(0.0)
        assertThat(ui.likes).isEqualTo(0)
        assertThat(ui.isLiked).isFalse()
    }

    @Test
    fun toUi_noMutatesSource() {
        val review = Review("x", 3, 4, 2.0, "c", "d", null, null, 5, false)
        val copyOfSource = review.copy()
        review.toUi()
        assertThat(review).isEqualTo(copyOfSource)
    }
}
package com.example.ratingroom.data.mappers

import com.example.ratingroom.data.models.Movie
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MovieMapperTest {

    @Test
    fun movie_toUi_mapeaTodosLosCampos() {
        val movie = Movie(
            id = 1,
            title = "Interstellar",
            year = "2014",
            genre = "Sci-Fi",
            rating = 4.9,
            reviews = 1200,
            description = "A journey through space and time",
            director = "Christopher Nolan",
            duration = "169min",
            imageUrl = "https://img",
            isFavorite = true
        )

        val ui = movie.toUi()
        val expected = MovieUi(
            id = 1,
            title = "Interstellar",
            year = "2014",
            genre = "Sci-Fi",
            rating = 4.9,
            reviews = 1200,
            description = "A journey through space and time",
            director = "Christopher Nolan",
            duration = "169min",
            imageUrl = "https://img",
            isFavorite = true
        )
        assertThat(ui).isEqualTo(expected)
    }

    @Test
    fun movie_toUi_manejaCamposOpcionalesNull() {
        val movie = Movie(
            id = 2,
            title = "No Image",
            year = "2020",
            genre = "Drama",
            rating = 3.5,
            reviews = 10,
            description = "Desc",
            director = "Dir",
            duration = "100min",
            imageUrl = null,
            isFavorite = false
        )
        val ui = movie.toUi()
        assertThat(ui.imageUrl).isNull()
        assertThat(ui.isFavorite).isFalse()
    }

    @Test
    fun listaMovie_toUi_mapeaColeccion() {
        val movies = listOf(
            Movie(1, "A", "2021", "Action", 4.0, 100, "d", "dir", "90min", null, false),
            Movie(2, "B", "2022", "Comedy", 2.0, 5, "d2", "dir2", "80min", "img", true)
        )
        val uiList = movies.toUiList()
        assertThat(uiList).hasSize(2)
        assertThat(uiList[0]).isEqualTo(MovieUi(1, "A", "2021", "Action", 4.0, 100, "d", "dir", "90min", null, false))
        assertThat(uiList[1]).isEqualTo(MovieUi(2, "B", "2022", "Comedy", 2.0, 5, "d2", "dir2", "80min", "img", true))
    }

    @Test
    fun toUi_conValoresBorde() {
        val movie = Movie(3, "Edge", "", "", 0.0, 0, "", "", "", null, false)
        val ui = movie.toUi()
        assertThat(ui.rating).isEqualTo(0.0)
        assertThat(ui.reviews).isEqualTo(0)
    }

    @Test
    fun toUi_noMutatesSource() {
        val movie = Movie(4, "X", "2023", "Thriller", 1.0, 2, "d", "dir", "70min", null, false)
        val copyOfSource = movie.copy()
        movie.toUi()
        assertThat(movie).isEqualTo(copyOfSource)
    }
}
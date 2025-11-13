package com.example.ratingroom.repository

import android.util.Log
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.dtos.MovieDto
import com.example.ratingroom.data.models.Movie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieFirebaseRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    
    private val TAG = "MovieFirebaseRepo"

    // ---------- Mappers Firebase -> Modelo ----------
    private fun mapFromFirestoreData(data: Map<String, Any>): MovieDto {
        return MovieDto(
            id = (data["id"] as? Number)?.toInt() ?: 0,
            title = data["title"] as? String ?: data["titulo"] as? String ?: "",
            year = data["year"] as? String ?: data["fechaSalida"] as? String ?: "",
            genre = data["genre"] as? String ?: data["subcategoria"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toDouble() ?: (data["averageRating"] as? Number)?.toDouble() ?: 0.0,
            reviews = (data["reviews"] as? Number)?.toInt() ?: (data["totalReviews"] as? Number)?.toInt() ?: 0,
            description = data["description"] as? String ?: data["descripcion"] as? String ?: "",
            director = data["director"] as? String ?: "",
            duration = data["duration"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String ?: data["portada"] as? String,
            favoritesCount = (data["favoritesCount"] as? Number)?.toInt() ?: 0
        )
    }

    private fun mapMovieFromFirestore(data: MovieDto): Movie {
        return Movie(
            id = data.id,
            title = data.title,
            year = data.year,
            genre = data.genre,
            rating = data.rating,
            reviews = data.reviews,
            description = data.description,
            director = data.director,
            duration = data.duration,
            imageUrl = data.imageUrl,
            favoritesCount = data.favoritesCount
        )
    }

    suspend fun getAllMovies(): Result<List<Movie>> {
        return try {
            Log.d(TAG, "🔥 Llamando a firestoreDataSource.getAllMovies()...")
            val firestoreMovies = firestoreDataSource.getAllMovies()
            Log.d(TAG, "🔥 Firestore devolvió ${firestoreMovies.size} documentos")
            
            val mappedMovies = firestoreMovies.mapNotNull { data ->
                try {
                    val movieDto = mapFromFirestoreData(data)
                    Log.d(TAG, "🔥 Mapeando película: ${movieDto.title}")
                    mapMovieFromFirestore(movieDto)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
            Log.d(TAG, "🔥 Mapeadas ${mappedMovies.size} películas exitosamente")
            Result.success(mappedMovies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMovieById(id: Int): Result<Movie?> {
        return try {
            val firestoreMovie = firestoreDataSource.getMovieById(id)
            val movie = firestoreMovie?.let { data ->
                try {
                    val movieDto = mapFromFirestoreData(data)
                    mapMovieFromFirestore(movieDto)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película $id desde Firebase: ${e.message}", e)
                    null
                }
            }
            Result.success(movie)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMoviesByGenre(genre: String): Result<List<Movie>> {
        return try {
            val firestoreMovies = firestoreDataSource.getMoviesByGenre(genre)
            val movies = firestoreMovies.mapNotNull { data ->
                try {
                    val movieDto = mapFromFirestoreData(data)
                    mapMovieFromFirestore(movieDto)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchMovies(query: String): Result<List<Movie>> {
        return try {
            val firestoreMovies = firestoreDataSource.searchMovies(query)
            val movies = firestoreMovies.mapNotNull { data ->
                try {
                    val movieDto = mapFromFirestoreData(data)
                    mapMovieFromFirestore(movieDto)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
            Result.success(movies)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGenres(): List<String> {
        val moviesResult = getAllMovies()
        return if (moviesResult.isSuccess) {
            val movies = moviesResult.getOrNull() ?: emptyList()
            listOf("Todos") + movies.mapNotNull { it.genre.takeIf { g -> g.isNotBlank() } }.distinct()
        } else {
            listOf("Todos")
        }
    }

    suspend fun getWatchLaterMovies(): List<Movie> = getAllMovies().getOrNull()?.take(2) ?: emptyList()
    suspend fun getFavoriteMovies(): List<Movie> = getAllMovies().getOrNull()?.filter { it.rating >= 4.7 } ?: emptyList()
    suspend fun getWatchedMovies(): List<Movie> = getAllMovies().getOrNull()?.takeLast(3) ?: emptyList()

    // ---------- Favoritos de películas ----------
    suspend fun toggleMovieFavorite(movieId: Int, userId: String): Result<Boolean> {
        return try {
            val wasFavorited = firestoreDataSource.toggleMovieFavorite(movieId, userId)
            Result.success(wasFavorited)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggleMovieFavorite: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun isMovieFavoriteByUser(movieId: Int, userId: String): Result<Boolean> {
        return try {
            val isFavorite = firestoreDataSource.isMovieFavoriteByUser(movieId, userId)
            Result.success(isFavorite)
        } catch (e: Exception) {
            Log.e(TAG, "Error isMovieFavoriteByUser: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getFavoriteMoviesCount(movieId: Int): Result<Int> {
        return try {
            val count = firestoreDataSource.getFavoriteMoviesCount(movieId)
            Result.success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getFavoriteMoviesCount: ${e.message}", e)
            Result.failure(e)
        }
    }
}

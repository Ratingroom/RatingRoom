package com.example.ratingroom.repository

import android.util.Log
import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.example.ratingroom.data.models.Movie
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieFirebaseRepository @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) {
    
    private val TAG = "MovieFirebaseRepo"

    // ---------- Mappers Firebase -> Modelo ----------
    private fun mapMovieFromFirestore(data: Map<String, Any>): Movie {
        val id = (data["id"] as? Number)?.toInt() ?: 0
        val title = data["title"] as? String ?: ""
        val description = data["description"] as? String ?: ""
        val year = data["year"] as? String ?: ""
        val genre = data["genre"] as? String ?: "Sin categoría"
        val director = data["director"] as? String ?: ""
        val duration = data["duration"] as? String ?: ""
        val imageUrl = data["imageUrl"] as? String
        val rating = (data["rating"] as? Number)?.toDouble() ?: 0.0
        val reviews = (data["reviews"] as? Number)?.toInt() ?: 0

        return Movie(
            id = id,
            title = title,
            year = year,
            genre = genre,
            rating = rating,
            reviews = reviews,
            description = description,
            director = director,
            duration = duration,
            imageUrl = imageUrl
        )
    }

    // ---------- API público ----------
    
    suspend fun getAllMovies(): List<Movie> {
        return try {
            Log.d(TAG, "🔥 Llamando a firestoreDataSource.getAllMovies()...")
            val firestoreMovies = firestoreDataSource.getAllMovies()
            Log.d(TAG, "🔥 Firestore devolvió ${firestoreMovies.size} documentos")
            
            val mappedMovies = firestoreMovies.mapNotNull { data ->
                try {
                    Log.d(TAG, "🔥 Mapeando película: ${data["title"] ?: "Sin título"}")
                    mapMovieFromFirestore(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
            Log.d(TAG, "🔥 Mapeadas ${mappedMovies.size} películas exitosamente")
            mappedMovies
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo películas desde Firebase: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getMovieById(id: Int): Movie? {
        return try {
            val firestoreMovie = firestoreDataSource.getMovieById(id)
            firestoreMovie?.let { data ->
                try {
                    mapMovieFromFirestore(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película $id desde Firebase: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo película $id desde Firebase: ${e.message}", e)
            null
        }
    }

    suspend fun getMoviesByGenre(genre: String): List<Movie> {
        return try {
            val firestoreMovies = firestoreDataSource.getMoviesByGenre(genre)
            firestoreMovies.mapNotNull { data ->
                try {
                    mapMovieFromFirestore(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo películas por género desde Firebase: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        return try {
            val firestoreMovies = firestoreDataSource.searchMovies(query)
            firestoreMovies.mapNotNull { data ->
                try {
                    mapMovieFromFirestore(data)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando película desde Firebase: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando películas desde Firebase: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getGenres(): List<String> {
        return try {
            val movies = getAllMovies()
            listOf("Todos") + movies.mapNotNull { it.genre.takeIf { g -> g.isNotBlank() } }.distinct()
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo géneros desde Firebase: ${e.message}", e)
            listOf("Todos")
        }
    }

    // Helpers para compatibilidad con UI
    suspend fun getWatchLaterMovies(): List<Movie> = getAllMovies().take(2)
    suspend fun getFavoriteMovies(): List<Movie> = getAllMovies().filter { it.rating >= 4.7 }
    suspend fun getWatchedMovies(): List<Movie> = getAllMovies().takeLast(3)
}

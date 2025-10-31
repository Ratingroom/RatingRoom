package com.example.ratingroom.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.ratingroom.data.models.Movie
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

class MoviePagingSource(
    private val firestore: FirebaseFirestore,
    private val genre: String? = null,
    private val searchQuery: String? = null,
    private val pageSize: Int = 6
) : PagingSource<DocumentSnapshot, Movie>() {

    private val TAG = "MoviePagingSource"

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, Movie> {
        return try {
            Log.d(TAG, " Cargando página, key: ${params.key?.id}")
            
            // Construir query base
            var query: Query = firestore.collection("movies")
                .orderBy("title") // Ordenar por título para paginación consistente
            
            // Aplicar filtro de género si existe
            if (genre != null && genre != "Todos") {
                query = query.whereEqualTo("genre", genre)
            }
            
            // Aplicar límite de página
            query = query.limit(pageSize.toLong())
            
            // Si hay una clave (documento anterior), empezar después de él
            params.key?.let { lastDocument ->
                query = query.startAfter(lastDocument)
            }
            
            // Ejecutar query
            val querySnapshot: QuerySnapshot = query.get().await()
            Log.d(TAG, " Recibidos ${querySnapshot.documents.size} documentos")
            
            // Mapear documentos a películas
            val movies = querySnapshot.documents.mapNotNull { doc ->
                try {
                    mapDocumentToMovie(doc)
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando documento ${doc.id}: ${e.message}", e)
                    null
                }
            }
            
            // Aplicar filtro de búsqueda en memoria si existe
            val filteredMovies = if (!searchQuery.isNullOrBlank()) {
                movies.filter { movie ->
                    movie.title.contains(searchQuery, ignoreCase = true) ||
                    movie.description.contains(searchQuery, ignoreCase = true) ||
                    movie.genre.contains(searchQuery, ignoreCase = true)
                }
            } else {
                movies
            }
            
            Log.d(TAG, " Mapeadas ${filteredMovies.size} películas")
            
            // Determinar la clave para la siguiente página
            val nextKey = if (querySnapshot.documents.size < pageSize) {
                null // No hay más páginas
            } else {
                querySnapshot.documents.lastOrNull()
            }
            
            LoadResult.Page(
                data = filteredMovies,
                prevKey = null, // No soportamos navegación hacia atrás en esta implementación
                nextKey = nextKey
            )
            
        } catch (e: Exception) {
            Log.e(TAG, " Error cargando películas: ${e.message}", e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, Movie>): DocumentSnapshot? {
        // Retornar null para empezar desde el principio al refrescar
        return null
    }
    
    private fun mapDocumentToMovie(doc: DocumentSnapshot): Movie {
        val data = doc.data ?: throw IllegalStateException("Documento sin datos")
        
        return Movie(
            id = (data["id"] as? Number)?.toInt() ?: doc.id.toIntOrNull() ?: 0,
            title = data["title"] as? String ?: data["titulo"] as? String ?: "",
            year = data["year"] as? String ?: data["fechaSalida"] as? String ?: "",
            genre = data["genre"] as? String ?: data["subcategoria"] as? String ?: "",
            rating = (data["rating"] as? Number)?.toDouble() 
                ?: (data["averageRating"] as? Number)?.toDouble() ?: 0.0,
            reviews = (data["reviews"] as? Number)?.toInt() 
                ?: (data["totalReviews"] as? Number)?.toInt() ?: 0,
            description = data["description"] as? String ?: data["descripcion"] as? String ?: "",
            director = data["director"] as? String ?: "",
            duration = data["duration"] as? String ?: "",
            imageUrl = data["imageUrl"] as? String ?: data["portada"] as? String
        )
    }
}

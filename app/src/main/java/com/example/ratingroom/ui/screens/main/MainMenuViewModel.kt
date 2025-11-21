package com.example.ratingroom.ui.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.repository.MovieRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val firestore: FirebaseFirestore,
    private val authRepository: com.example.ratingroom.repository.AuthRepository
) : ViewModel() {

    private val TAG = "MainMenuViewModel"
    
    private val _uiState = MutableStateFlow(
        MainMenuUIState(
            isLoading = true,
            movies = emptyList(),
            filteredMovies = emptyList(),
            genres = listOf("Todos"),
            searchQuery = "",
            selectedGenre = "Todos",
            filterExpanded = false,
            errorMessage = null,
            usePagination = true,
            currentPage = 0,
            hasMorePages = true,
            pageSize = 6
        )
    )
    val uiState: StateFlow<MainMenuUIState> = _uiState.asStateFlow()
    
    // Cursores para paginación de Firestore
    private var lastDocument: DocumentSnapshot? = null
    private var firstDocument: DocumentSnapshot? = null
    private val pageHistory = mutableListOf<DocumentSnapshot?>() // Historial de primeros docs de cada página

    init {
        loadGenres()
        loadMoviesPage()
    }

    /** Carga los géneros disponibles */
    private fun loadGenres() {
        viewModelScope.launch {
            try {
                val genres = movieRepository.getGenres().ifEmpty { listOf("Todos") }
                _uiState.value = _uiState.value.copy(genres = genres)
            } catch (e: Exception) {
                Log.e(TAG, "Error cargando géneros: ${e.message}", e)
            }
        }
    }

    /** Carga una página de películas con paginación de Firestore */
    fun loadMoviesPage(startAfter: DocumentSnapshot? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val state = _uiState.value
                
                // Construir query base como Query (no CollectionReference)
                var query: com.google.firebase.firestore.Query = firestore.collection("movies")
                
                // Aplicar filtro de género si no es "Todos"
                if (state.selectedGenre != "Todos") {
                    query = query.whereEqualTo("genre", state.selectedGenre)
                }
                
                // Ordenar por ID del documento (siempre disponible y único)
                // Esto permite la paginación correcta
                query = query.orderBy(com.google.firebase.firestore.FieldPath.documentId())
                
                // Aplicar paginación
                query = query.limit(state.pageSize.toLong())
                
                // Si hay un cursor, empezar después de él
                startAfter?.let {
                    query = query.startAfter(it)
                }
                
                // Ejecutar query
                val querySnapshot = query.get().await()
                Log.d(TAG, "📦 Recibidos ${querySnapshot.documents.size} documentos")
                
                // Mapear documentos a películas (en paralelo para mejor rendimiento)
                val movies = querySnapshot.documents.map { doc ->
                    async {
                        try {
                            mapDocumentToMovie(doc)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapeando documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                }.mapNotNull { it.await() }
                
                // Aplicar filtro de búsqueda en memoria si existe
                val filteredMovies = if (state.searchQuery.isNotBlank()) {
                    movies.filter { movie ->
                        movie.title.contains(state.searchQuery, ignoreCase = true) ||
                        movie.description.contains(state.searchQuery, ignoreCase = true) ||
                        movie.genre.contains(state.searchQuery, ignoreCase = true)
                    }
                } else {
                    movies
                }
                
                // Ordenar: películas destacadas primero, luego por título
                val sortedMovies = filteredMovies.sortedWith(
                    compareByDescending<Movie> { it.isFeatured }
                        .thenBy { it.title }
                )
                
                // Actualizar cursores basados en el ÚLTIMO documento de Firebase
                // (no el último según nuestro ordenamiento personalizado)
                if (querySnapshot.documents.isNotEmpty()) {
                    firstDocument = querySnapshot.documents.firstOrNull()
                    lastDocument = querySnapshot.documents.lastOrNull()
                }
                
                // Determinar si hay más páginas
                val hasMore = querySnapshot.documents.size >= state.pageSize
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    movies = sortedMovies,
                    filteredMovies = sortedMovies,
                    hasMorePages = hasMore,
                    errorMessage = if (sortedMovies.isEmpty()) "No hay películas disponibles" else null
                )
                
                Log.d(TAG, "✅ Cargadas ${sortedMovies.size} películas, hay más: $hasMore")
                Log.d(TAG, "⭐ Películas destacadas: ${sortedMovies.count { it.isFeatured }}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error cargando películas: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Error cargando películas"
                )
            }
        }
    }
    
    /** Navegar a la siguiente página */
    fun nextPage() {
        val state = _uiState.value
        if (state.hasMorePages && !state.isLoading) {
            // Guardar el primer documento de la página actual en el historial
            pageHistory.add(firstDocument)
            
            _uiState.value = state.copy(currentPage = state.currentPage + 1)
            loadMoviesPage(startAfter = lastDocument)
        }
    }
    
    /** Navegar a la página anterior */
    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 0 && !state.isLoading) {
            // Retroceder una página usando el historial
            val newPage = state.currentPage - 1
            
            if (newPage == 0) {
                // Volver a la primera página
                pageHistory.clear()
                lastDocument = null
                firstDocument = null
                _uiState.value = state.copy(currentPage = 0)
                loadMoviesPage()
            } else {
                // Cargar desde el documento guardado
                val startDoc = if (pageHistory.size > newPage) {
                    pageHistory[newPage]
                } else {
                    null
                }
                
                // Eliminar páginas posteriores del historial
                while (pageHistory.size > newPage) {
                    pageHistory.removeAt(pageHistory.size - 1)
                }
                
                _uiState.value = state.copy(currentPage = newPage)
                loadMoviesPage(startAfter = startDoc)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, currentPage = 0)
        // Reiniciar paginación con búsqueda
        pageHistory.clear()
        lastDocument = null
        firstDocument = null
        loadMoviesPage()
    }

    fun onGenreSelected(genre: String) {
        _uiState.value = _uiState.value.copy(
            selectedGenre = genre,
            filterExpanded = false,
            currentPage = 0
        )
        // Reiniciar paginación con nuevo género
        pageHistory.clear()
        lastDocument = null
        firstDocument = null
        loadMoviesPage()
    }

    fun onFilterExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(filterExpanded = expanded)
    }

    /** Por si quieres recargar desde UI */
    fun refresh() {
        _uiState.value = _uiState.value.copy(currentPage = 0)
        pageHistory.clear()
        lastDocument = null
        firstDocument = null
        loadMoviesPage()
    }
    
    private suspend fun mapDocumentToMovie(doc: DocumentSnapshot): Movie {
        val data = doc.data ?: throw IllegalStateException("Documento sin datos")
        
        val favoritesCount = (data["favoritesCount"] as? Number)?.toInt() ?: 0
        
        // Calcular si es destacada (en paralelo para eficiencia)
        val isFeatured = calculateIfFeatured(favoritesCount)
        
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
            imageUrl = data["imageUrl"] as? String ?: data["portada"] as? String,
            favoritesCount = favoritesCount,
            isFeatured = isFeatured
        )
    }

    /**
     * Calcula si una película es destacada basándose en su número de favoritos.
     * Una película es destacada si tiene el mayor número de favoritos (>= 1).
     * En caso de empate, todas las películas empatadas son destacadas.
     */
    private suspend fun calculateIfFeatured(favoritesCount: Int): Boolean {
        // Si no tiene favoritos, definitivamente no es destacada
        if (favoritesCount < 1) return false
        
        return try {
            // Buscar el máximo número de favoritos en toda la colección
            val maxFavoritesSnapshot = firestore.collection("movies")
                .orderBy("favoritesCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            val maxFavorites = maxFavoritesSnapshot.documents.firstOrNull()
                ?.get("favoritesCount") as? Number
            
            // Es destacada si tiene el máximo número de favoritos
            maxFavorites?.toInt() == favoritesCount
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando película destacada: ${e.message}", e)
            false
        }
    }

    fun toggleMovieFavorite(movieId: Int) {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                if (userId == "anonymous") {
                    _uiState.value = _uiState.value.copy(errorMessage = "Debes iniciar sesión para agregar favoritos")
                    return@launch
                }
                
                val result = movieRepository.toggleMovieFavorite(movieId, userId)
                if (result.isSuccess) {
                    // Recargar la página actual para reflejar el cambio
                    refresh()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun getCurrentUserId(): String = authRepository.currentUser?.uid ?: "anonymous"
}

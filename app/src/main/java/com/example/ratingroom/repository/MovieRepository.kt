package com.example.ratingroom.repository

import android.util.Log
import com.example.ratingroom.data.models.Movie
import com.example.ratingroom.data.models.Review
import com.example.ratingroom.data.models.User
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

object MovieRepository {

    private const val TAG = "MovieRepo"

    // Cambia si corres en dispositivo físico
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private interface ApiService {
        @GET("api/peliculas")
        suspend fun getPeliculasRaw(): JsonElement

        @GET("api/peliculas/{id}")
        suspend fun getPeliculaRaw(@Path("id") id: Int): JsonElement

        @GET("api/peliculas/{id}/reviews")
        suspend fun getReviewsRaw(@Path("id") id: Int): JsonElement

        @GET("api/usuarios/{id}")
        suspend fun getUsuarioRaw(@Path("id") id: Int): JsonElement
    }

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // ---------- helpers de navegación segura del JSON ----------
    private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonElement.asJsonArrayOrNull(): JsonArray? =
        if (this != null && this.isJsonArray) this.asJsonArray else null

    private fun JsonObject.safeString(name: String): String? =
        if (has(name) && get(name).isJsonPrimitive) get(name).asString else null

    private fun JsonObject.safeDouble(name: String): Double? =
        safeString(name)?.toDoubleOrNull()

    private fun JsonObject.safeInt(name: String): Int? =
        try {
            when {
                has(name) && get(name).isJsonPrimitive -> get(name).asInt
                else -> null
            }
        } catch (e: Exception) {
            null
        }

    // Dado un root JsonElement intenta extraer un JsonArray por varias rutas comunes:
    // - Si root es array -> devuelve root
    // - Si root is object -> busca data, data.peliculas, peliculas, rows, results, items
    // - Si paths se pasan (ej. "data","rows") intenta navegar en ese orden y devolver resultado
    private fun asArrayFlexible(root: JsonElement?, vararg paths: String): JsonArray {
        if (root == null) return JsonArray()

        // caso raíz array
        root.asJsonArrayOrNull()?.let { return it }

        val obj = root.asJsonObjectOrNull() ?: JsonObject()

        // si el usuario pasó una ruta específica (ej "data","peliculas"), navega por ella
        if (paths.isNotEmpty()) {
            var current: JsonElement? = obj
            for (p in paths) {
                current = (current?.asJsonObjectOrNull())?.let { o -> if (o.has(p)) o.get(p) else null }
            }
            current?.asJsonArrayOrNull()?.let { return it }
            // si llegó a un objeto con "peliculas" dentro, probar
            current?.asJsonObjectOrNull()?.let { co ->
                if (co.has("peliculas")) co.getAsJsonArray("peliculas")?.let { return it }
            }
        }

        // candidatos comunes
        val candidates = listOf("data", "peliculas", "results", "rows", "items")
            .mapNotNull { key -> if (obj.has(key)) obj.get(key) else null }

        for (cand in candidates) {
            // si candidato es array, devuelve
            cand.asJsonArrayOrNull()?.let { return it }
            // si candidato es objeto y tiene "peliculas" u "items" dentro, intenta esas rutas
            cand.asJsonObjectOrNull()?.let { co ->
                if (co.has("peliculas")) {
                    co.getAsJsonArray("peliculas")?.let { return it }
                }
                if (co.has("rows")) co.getAsJsonArray("rows")?.let { return it }
                if (co.has("items")) co.getAsJsonArray("items")?.let { return it }
                if (co.has("data")) co.getAsJsonArray("data")?.let { return it }
            }
        }

        // nothing -> retornar array vacío para evitar crashes
        return JsonArray()
    }

    // Extrae un objeto de formas comunes: {...} | { data: {...} } | [ {...} ]
    private fun asObjectFlexible(root: JsonElement?): JsonObject {
        if (root == null) return JsonObject()
        root.asJsonObjectOrNull()?.let { o ->
            // si viene envuelto en data o item
            o.safeString("dummy") // (no-op) solo para claridad
            return when {
                o.has("data") -> asObjectFlexible(o.get("data"))
                o.has("item") -> asObjectFlexible(o.get("item"))
                o.has("pelicula") -> asObjectFlexible(o.get("pelicula"))
                else -> o
            }
        }
        root.asJsonArrayOrNull()?.let { arr ->
            if (arr.size() > 0) return asObjectFlexible(arr[0])
        }
        return JsonObject()
    }

    // ---------- mappers ----------
    private fun mapMovie(obj: JsonObject): Movie {
        val id = obj.safeInt("id") ?: 0
        val titulo = obj.safeString("titulo") ?: obj.safeString("title") ?: ""
        val descripcion = obj.safeString("descripcion") ?: obj.safeString("description") ?: ""
        val fecha = obj.safeString("fechaSalida") ?: obj.safeString("fecha") ?: ""
        val subcat = obj.safeString("subcategoria") ?: obj.safeString("subCategory") ?: "Sin categoría"
        val portada = obj.safeString("portada") ?: obj.safeString("imageUrl")
        val avg = obj.safeDouble("averageRating") ?: obj.safeDouble("rating") ?: 0.0
        val totalReviews = obj.safeInt("totalReviews") ?: obj.safeInt("reviews") ?: 0

        return Movie(
            id = id,
            title = titulo,
            year = fecha,
            genre = subcat,
            rating = avg,
            reviews = totalReviews,
            description = descripcion,
            director = "", // no viene del backend por ahora
            duration = "",
            imageUrl = portada
        )
    }

    private fun mapReview(obj: JsonObject): Review {
        return Review(
            id = obj.safeInt("id") ?: 0,
            movieId = obj.safeInt("pelicula_id") ?: obj.safeInt("peliculaId") ?: 0,
            userId = obj.safeInt("usuario_id") ?: obj.safeInt("userId") ?: 0,
            rating = (obj.safeDouble("rating") ?: obj.safeDouble("valor") ?: 0.0),
            comment = obj.safeString("texto") ?: obj.safeString("comment") ?: "",
            date = obj.safeString("createdAt") ?: ""
        )
    }

    private fun mapUser(obj: JsonObject): User {
        return User(
            id = obj.safeInt("id") ?: 0,
            displayName = obj.safeString("username") ?: obj.safeString("displayName") ?: "",
            email = obj.safeString("email") ?: "",
            biography = "",
            location = "",
            favoriteGenre = ""
        )
    }

    // ---------- API público (suspend) ----------

    /**
     * Devuelve lista de películas.
     * Soporta respuestas:
     *  - [ {...}, {...} ]
     *  - { data: { peliculas: [ ... ] } }
     *  - { data: [ ... ] }
     *  - { peliculas: [ ... ] }
     */
    suspend fun getAllMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val root = try {
            api.getPeliculasRaw()
        } catch (e: Exception) {
            Log.e(TAG, "Error llamando API /api/peliculas: ${e.message}", e)
            return@withContext emptyList()
        }

        // Primero intenta rutas comunes (data -> peliculas)
        val arr = asArrayFlexible(root, "data", "peliculas")
        val effective = if (arr.size() == 0) asArrayFlexible(root) else arr

        if (effective.size() == 0) {
            // debug: imprime exacto JSON que vino para que lo revises en Logcat
            Log.e(TAG, "Respuesta inesperada de /api/peliculas: $root")
        }

        return@withContext effective.mapNotNull { el ->
            el.asJsonObjectOrNull()?.let { mapMovie(it) }
        }
    }

    suspend fun getMoviesByGenre(genre: String): List<Movie> {
        val all = getAllMovies()
        return if (genre == "Todos") all else all.filter { it.genre.equals(genre, ignoreCase = true) }
    }

    suspend fun searchMovies(query: String): List<Movie> {
        val q = query.trim()
        if (q.isEmpty()) return getAllMovies()
        return getAllMovies().filter {
            it.title.contains(q, ignoreCase = true) ||
                    it.description.contains(q, ignoreCase = true) ||
                    it.genre.contains(q, ignoreCase = true)
        }
    }

    suspend fun getMovieById(id: Int): Movie? = withContext(Dispatchers.IO) {
        // Intento directo al endpoint de detalle
        val direct: Movie? = try {
            val root = api.getPeliculaRaw(id)
            val obj = asObjectFlexible(root)
            mapMovie(obj)
        } catch (e: Exception) {
            Log.e(TAG, "Error /api/peliculas/$id: ${e.message}", e)
            null
        }

        // Si el resultado directo es válido (tiene título), úsalo
        if (direct != null && direct.title.isNotBlank()) return@withContext direct

        // Fallback: buscar en la lista general por id
        return@withContext try {
            val all = getAllMovies()
            val fromList = all.firstOrNull { it.id == id }
            if (fromList == null) {
                Log.w(TAG, "Fallback: película $id no encontrada en lista")
            } else {
                Log.d(TAG, "Fallback: película $id encontrada en lista")
            }
            fromList
        } catch (e: Exception) {
            Log.e(TAG, "Error en fallback getAllMovies: ${e.message}", e)
            null
        }
    }

    suspend fun getGenres(): List<String> = withContext(Dispatchers.IO) {
        val movies = getAllMovies()
        listOf("Todos") + movies.mapNotNull { it.genre.takeIf { g -> g.isNotBlank() } }.distinct()
    }

    suspend fun getUserById(id: Int): User? = withContext(Dispatchers.IO) {
        val root = try {
            api.getUsuarioRaw(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error /api/usuarios/$id: ${e.message}", e)
            return@withContext null
        }
        val obj = asObjectFlexible(root)
        return@withContext try { mapUser(obj) } catch (e: Exception) { null }
    }

    suspend fun getReviewsForMovie(movieId: Int): List<Review> = withContext(Dispatchers.IO) {
        val root = try {
            api.getReviewsRaw(movieId)
        } catch (e: Exception) {
            Log.e(TAG, "Error /api/peliculas/$movieId/reviews: ${e.message}", e)
            return@withContext emptyList()
        }

        val arr = asArrayFlexible(root, "data", "reviews")
        val effective = if (arr.size() == 0) asArrayFlexible(root) else arr

        return@withContext effective.mapNotNull { it.asJsonObjectOrNull()?.let(::mapReview) }
    }

    // helpers UI
    suspend fun getWatchLaterMovies(): List<Movie> = getAllMovies().take(2)
    suspend fun getFavoriteMovies(): List<Movie> = getAllMovies().filter { it.rating >= 4.7 }
    suspend fun getWatchedMovies(): List<Movie> = getAllMovies().takeLast(3)
}

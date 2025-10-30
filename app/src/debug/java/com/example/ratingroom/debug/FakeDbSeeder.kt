package com.example.ratingroom.debug

import android.util.Log
import io.github.serpro69.kfaker.Faker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

// Eliminado: val io: Any  // causaba conflicto con el paquete `io` en los imports

/**
 * Seeder de base de datos falsa para entorno DEBUG.
 * Genera usuarios, relaciones de seguidores/seguidos y reseñas.
 * Idempotente: crea una marca en /meta/seed_debug_v1 para evitar duplicados.
 */
object FakeDbSeeder {
    private const val SEED_MARK = "seed_debug_v1"

    @JvmStatic
    fun run(firestore: FirebaseFirestore) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val metaDoc = firestore.collection("meta").document(SEED_MARK).get().await()
                if (metaDoc.exists()) {
                    Log.i("FakeDbSeeder", "Seed ya ejecutado; saltando.")
                    return@launch
                }

                seed(firestore)

                firestore.collection("meta").document(SEED_MARK)
                    .set(mapOf("at" to FieldValue.serverTimestamp()))
                    .await()

                Log.i("FakeDbSeeder", "Seed completado correctamente.")
            } catch (e: Exception) {
                Log.e("FakeDbSeeder", "Error en seeding: ${e.message}", e)
            }
        }
    }

    private suspend fun seed(firestore: FirebaseFirestore) {
        val faker = try {
            Faker()
        } catch (e: Throwable) {
            Log.w("FakeDbSeeder", "Faker no disponible, usando Random.")
            null
        }

        // Payload para Realtime Database y export JSON
        val rtdbPayload = mutableMapOf<String, Any>()
        val rtdbUsers = mutableMapOf<String, Map<String, Any>>()
        val rtdbFollowers = mutableMapOf<String, MutableMap<String, Boolean>>()
        val rtdbFollowing = mutableMapOf<String, MutableMap<String, Boolean>>()
        val rtdbReviews = mutableMapOf<String, Map<String, Any>>()
        val rtdbMovies = mutableMapOf<String, Map<String, Any>>()

        // 1) Usuarios
        val userIds = mutableListOf<String>()
        val usersData = mutableMapOf<String, Map<String, Any>>()
        val genres = listOf("Acción", "Drama", "Sci-Fi", "Comedia", "Terror", "Romance")
        repeat(20) {
            val uid = UUID.randomUUID().toString()
            userIds += uid
            val fullName = faker?.name?.name() ?: "${fakerLikeFirst()} ${fakerLikeLast()}"
            val email = faker?.internet?.email() ?: "${fullName.lowercase().replace(" ", ".")}@mail.com"
            val favorite = genres.random()
            val now = System.currentTimeMillis()
            val data = mapOf(
                "email" to email,
                "fullName" to fullName,
                "favoriteGenre" to favorite,
                "createdAt" to now,
                "updatedAt" to now,
                "followersCount" to 0,
                "followingCount" to 0
            )
            usersData[uid] = data
            rtdbUsers[uid] = data
        }

        // Escribir usuarios a Firestore
        for ((uid, data) in usersData) {
            firestore.collection("users").document(uid).set(data).await()
        }

        // 2) Relaciones seguidores/seguidos (simétricas)
        val edges = mutableListOf<Pair<String, String>>()
        val maxEdges = userIds.size * 2
        repeat(maxEdges) {
            val a = userIds.random()
            val b = userIds.random()
            if (a == b) return@repeat
            // evitar duplicados
            if (edges.any { it.first == a && it.second == b }) return@repeat
            edges += a to b
        }

        // Aplicar edges y actualizar contadores
        val followersCount = mutableMapOf<String, Int>().withDefault { 0 }
        val followingCount = mutableMapOf<String, Int>().withDefault { 0 }
        for ((viewer, target) in edges) {
            firestore.collection("users").document(viewer)
                .collection("following").document(target)
                .set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                .await()
            firestore.collection("users").document(target)
                .collection("followers").document(viewer)
                .set(mapOf("timestamp" to FieldValue.serverTimestamp()))
                .await()
            followingCount[viewer] = (followingCount[viewer] ?: 0) + 1
            followersCount[target] = (followersCount[target] ?: 0) + 1

            // RTDB mirrors
            val fMap = rtdbFollowers.getOrPut(target) { mutableMapOf() }
            fMap[viewer] = true
            val gMap = rtdbFollowing.getOrPut(viewer) { mutableMapOf() }
            gMap[target] = true
        }
        // Persistir contadores en Firestore
        for (uid in userIds) {
            firestore.collection("users").document(uid)
                .update(
                    mapOf(
                        "followersCount" to (followersCount[uid] ?: 0),
                        "followingCount" to (followingCount[uid] ?: 0)
                    )
                ).await()
        }

        // 3) Reseñas por usuario y película
        val moviePool = (1..30).toList()
        for (uid in userIds) {
            val reviewsPerUser = Random.nextInt(2, 6)
            repeat(reviewsPerUser) {
                val movieId = moviePool.random()
                val rating = Random.nextInt(1, 6)
                val words = faker?.lorem?.words() ?: "Muy buena película"
                val text = "$words (${rating}/5)"
                // fanout Firestore
                val reviewData = mapOf(
                    "userId" to uid,
                    "movieId" to movieId,
                    "rating" to rating,
                    "text" to text,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                val reviewDoc = firestore.collection("reviews").add(reviewData).await()

                firestore.collection("users").document(uid)
                    .collection("reviews").document(reviewDoc.id)
                    .set(reviewData).await()

                val movieUpdate = mapOf("updatedAt" to FieldValue.serverTimestamp())
                firestore.collection("movies").document(movieId.toString())
                    .set(movieUpdate, com.google.firebase.firestore.SetOptions.merge())
                    .await()
                firestore.collection("movies").document(movieId.toString())
                    .collection("reviews").document(reviewDoc.id)
                    .set(reviewData).await()

                // RTDB mirrors
                val rReview = mapOf(
                    "id" to reviewDoc.id,
                    "userId" to uid,
                    "movieId" to movieId,
                    "rating" to rating,
                    "text" to text,
                    "createdAt" to System.currentTimeMillis()
                )
                rtdbReviews[reviewDoc.id] = rReview
                rtdbMovies[movieId.toString()] = mapOf("updatedAt" to System.currentTimeMillis())
            }
        }

        // Construir payload RTDB
        rtdbPayload["users"] = rtdbUsers
        rtdbPayload["followers"] = rtdbFollowers
        rtdbPayload["following"] = rtdbFollowing
        rtdbPayload["reviews"] = rtdbReviews
        rtdbPayload["movies"] = rtdbMovies

        // Subir a Realtime Database (rama /seed/debug_v1)
        try {
            val db = FirebaseDatabase.getInstance()
            db.reference.child("seed").child("debug_v1").setValue(rtdbPayload).await()
            Log.i("FakeDbSeeder", "RTDB seed subido en /seed/debug_v1")
        } catch (e: Exception) {
            Log.w("FakeDbSeeder", "No se pudo subir a RTDB: ${e.message}")
        }

        // Export JSON (para Firebase CLI database:set)
        val json = try { Gson().toJson(rtdbPayload) } catch (e: Exception) { null }
        json?.let {
            Log.i("FakeDbSeeder", "RTDB JSON export (cópialo a un archivo si lo necesitas):\n$it")
        }
    }

    private fun fakerLikeFirst(): String {
        val first = listOf("Juan", "Ana", "Luis", "María", "Pedro", "Lucía")
        return first.random()
    }
    private fun fakerLikeLast(): String {
        val last = listOf("García", "Martínez", "López", "González", "Rodríguez", "Hernández")
        return last.random()
    }
}
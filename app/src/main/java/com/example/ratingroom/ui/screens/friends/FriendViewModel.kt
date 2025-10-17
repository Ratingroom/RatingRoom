package com.example.ratingroom.ui.screens.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.dtos.ReviewDto
import com.example.ratingroom.repository.AuthRepository
import com.example.ratingroom.ui.screens.profile.ProfileData
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(FriendUIState())
    val ui: StateFlow<FriendUIState> = _ui.asStateFlow()

    private var targetUid: String? = null
    private var regFollowers: ListenerRegistration? = null
    private var regFollowing: ListenerRegistration? = null
    private var regAmIFollowing: ListenerRegistration? = null
    private var regUserReviews: ListenerRegistration? = null

    fun load(userId: String) {
        targetUid = userId
        _ui.value = _ui.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                val doc = firestore.collection("users").document(userId).get().await()
                if (!doc.exists()) error("Usuario no encontrado")

                val name = (doc.get("fullName") as? String).orEmpty().ifBlank { "Usuario" }
                val email = doc.get("email") as? String
                val favorite = doc.get("favoriteGenre") as? String
                val createdAt = (doc.get("createdAt") as? Number)?.toLong()
                val photo = doc.get("profileImageUrl") as? String
                val memberSince = createdAt?.let {
                    SimpleDateFormat("MMM yyyy", Locale("es")).format(Date(it))
                }

                ProfileData(
                    name = name,
                    email = email,
                    memberSince = memberSince,
                    favoriteGenre = favorite,
                    reviewsCount = 0,        // se recalcula con las reseñas en vivo
                    averageRating = 0.0,     // idem
                    profileImageUrl = photo,
                    mainMovieId = (doc.get("mainMovieId") as? Number)?.toInt(),
                    followersCount = (doc.get("followersCount") as? Number)?.toInt() ?: 0,
                    followingCount = (doc.get("followingCount") as? Number)?.toInt() ?: 0
                )
            }.onSuccess { profile ->
                _ui.value = _ui.value.copy(isLoading = false, profile = profile)
                observeCounts()
                observeAmIFollowing()
                observeUserReviews()   // ⬅️ reseñas en tiempo real
            }.onFailure { e ->
                _ui.value = _ui.value.copy(isLoading = false, errorMessage = e.message ?: "Error")
            }
        }
    }

    /* ---------------- Seguimiento / contadores ---------------- */

    private fun observeCounts() {
        clearCountListeners()
        val uid = targetUid ?: return

        regFollowers = firestore.collection("users").document(uid)
            .collection("followers")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("FriendVM", "followers: ${err.message}", err); return@addSnapshotListener }
                _ui.value = _ui.value.copy(followersCount = snap?.size() ?: 0)
            }

        regFollowing = firestore.collection("users").document(uid)
            .collection("following")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("FriendVM", "following: ${err.message}", err); return@addSnapshotListener }
                _ui.value = _ui.value.copy(followingCount = snap?.size() ?: 0)
            }
    }

    private fun observeAmIFollowing() {
        regAmIFollowing?.remove()
        val me = authRepo.currentUser?.uid ?: return
        val target = targetUid ?: return
        regAmIFollowing = firestore.collection("users")
            .document(me).collection("following").document(target)
            .addSnapshotListener { snap, _ ->
                _ui.value = _ui.value.copy(isFollowing = snap?.exists() == true)
            }
    }

    fun toggleFollow() {
        val me = authRepo.currentUser?.uid ?: return
        val target = targetUid ?: return
        viewModelScope.launch {
            runCatching {
                val myFollowing = firestore.collection("users").document(me)
                    .collection("following").document(target)
                val isFollowingNow = _ui.value.isFollowing
                if (isFollowingNow) {
                    myFollowing.delete().await()
                    firestore.collection("users").document(target)
                        .collection("followers").document(me).delete().await()
                } else {
                    myFollowing.set(mapOf("createdAt" to System.currentTimeMillis())).await()
                    firestore.collection("users").document(target)
                        .collection("followers").document(me)
                        .set(mapOf("createdAt" to System.currentTimeMillis())).await()
                }
            }.onFailure { e -> Log.e("FriendVM", "toggleFollow: ${e.message}", e) }
        }
    }

    fun openFollowers() {
        _ui.value = _ui.value.copy(showFollowers = true)
        loadNames("followers") { _ui.value = _ui.value.copy(followersNames = it) }
    }
    fun openFollowing() {
        _ui.value = _ui.value.copy(showFollowing = true)
        loadNames("following") { _ui.value = _ui.value.copy(followingNames = it) }
    }
    fun closeDialogs() {
        _ui.value = _ui.value.copy(showFollowers = false, showFollowing = false)
    }

    private fun loadNames(coll: String, done: (List<String>) -> Unit) {
        val uid = targetUid ?: return
        viewModelScope.launch {
            runCatching {
                val ids = firestore.collection("users").document(uid)
                    .collection(coll).get().await().documents.map { it.id }
                if (ids.isEmpty()) { done(emptyList()); return@launch }
                val names = mutableListOf<String>()
                ids.chunked(10).forEach { chunk ->
                    val snap = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), chunk).get().await()
                    snap.documents.forEach { d ->
                        val n = (d.get("fullName") as? String)?.trim().orEmpty()
                        if (n.isNotEmpty()) names.add(n)
                    }
                }
                done(names.sorted())
            }.onFailure { done(emptyList()) }
        }
    }

    private fun clearCountListeners() {
        regFollowers?.remove(); regFollowers = null
        regFollowing?.remove(); regFollowing = null
    }

    /* ---------------- Reseñas del usuario visitado ---------------- */

    private fun observeUserReviews() {
        regUserReviews?.remove()
        val uid = targetUid ?: return

        _ui.value = _ui.value.copy(isLoadingReviews = true)
        regUserReviews = firestore.collection("users")
            .document(uid)
            .collection("reviews")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("FriendVM", "observeUserReviews: ${err.message}", err)
                    _ui.value = _ui.value.copy(isLoadingReviews = false)
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { d ->
                    // Mapeo seguro a tu DTO
                    ReviewDto(
                        id = d.getString("id") ?: d.id,
                        usuario_id = 0, // no lo usas aquí; si quieres, guarda el hash del uid
                        pelicula_id = (d.get("movieId") as? Number)?.toInt() ?: 0,
                        rating = (d.get("rating") as? Number)?.toInt() ?: 0,
                        texto = d.getString("text") ?: "",
                        // extras desnormalizados si existen:
                        userName = d.getString("userName"),
                        userImageUrl = d.getString("userImageUrl"),
                        likes = (d.get("likes") as? Number)?.toInt() ?: 0
                    )
                }.sortedByDescending { it.id } // ordena como prefieras; si tienes createdAt, usa eso

                val avg = if (list.isNotEmpty()) list.map { it.rating }.average() else 0.0
                _ui.value = _ui.value.copy(
                    isLoadingReviews = false,
                    reviews = list,
                    profile = _ui.value.profile?.copy(
                        reviewsCount = list.size,
                        averageRating = avg
                    )
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        clearCountListeners()
        regAmIFollowing?.remove(); regAmIFollowing = null
        regUserReviews?.remove(); regUserReviews = null
    }
}

package com.example.ratingroom.data.datasource.impl

import com.example.ratingroom.data.datasource.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreDataSourceImpl @Inject constructor(
    private val firestoreService: FirebaseFirestore,
    private val authService: FirebaseAuth
) : FirestoreDataSource {

    private val currentUserId: String?
        get() = authService.currentUser?.uid

    override suspend fun updateUserProfile(
        displayName: String?,
        email: String?,
        biography: String?,
        location: String?,
        favoriteGenre: String?,
        birthdate: String?,
        website: String?,
        profileImageUrl: String?
    ) {
        println("FirestoreDataSource.updateUserProfile: Iniciando actualización de perfil")
        println("FirestoreDataSource.updateUserProfile: profileImageUrl recibido: $profileImageUrl")
        
        val userId = currentUserId ?: throw IllegalStateException("Usuario no autenticado")
        println("FirestoreDataSource.updateUserProfile: Usuario autenticado: $userId")
        
        // Preparar datos para actualizar en Firestore
        val updates = mutableMapOf<String, Any>()
        displayName?.let { updates["fullName"] = it }
        email?.let { updates["email"] = it }
        biography?.let { updates["biography"] = it }
        location?.let { updates["location"] = it }
        favoriteGenre?.let { updates["favoriteGenre"] = it }
        birthdate?.let { updates["birthdate"] = it }
        website?.let { updates["website"] = it }
        profileImageUrl?.let { 
            println("FirestoreDataSource.updateUserProfile: Añadiendo profileImageUrl a updates: $it")
            updates["profileImageUrl"] = it 
        }
        updates["updatedAt"] = System.currentTimeMillis()
        
        println("FirestoreDataSource.updateUserProfile: Campos a actualizar en Firestore: ${updates.keys.joinToString()}")
        
        if (updates.isNotEmpty()) {
            println("FirestoreDataSource.updateUserProfile: Actualizando documento en Firestore para usuario: $userId")
            try {
                // Verificar si el documento existe
                val docRef = firestoreService.collection("users").document(userId)
                val docSnapshot = docRef.get().await()
                
                if (docSnapshot.exists()) {
                    println("FirestoreDataSource.updateUserProfile: Documento existe, actualizando...")
                    docRef.update(updates).await()
                    println("FirestoreDataSource.updateUserProfile: Documento actualizado exitosamente en Firestore")
                } else {
                    println("FirestoreDataSource.updateUserProfile: Documento NO existe, creando nuevo documento...")
                    docRef.set(updates).await()
                    println("FirestoreDataSource.updateUserProfile: Documento creado exitosamente en Firestore")
                }
            } catch (e: Exception) {
                println("FirestoreDataSource.updateUserProfile: ERROR al actualizar documento en Firestore: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    override suspend fun getUserProfile(): Map<String, Any>? {
        val userId = currentUserId ?: return null
        println("FirestoreDataSource.getUserProfile: Obteniendo perfil para usuario: $userId")
        
        return try {
            val document = firestoreService.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                println("FirestoreDataSource.getUserProfile: Documento encontrado en Firestore")
                val data = document.data
                println("FirestoreDataSource.getUserProfile: Datos recuperados: ${data?.keys?.joinToString()}")
                println("FirestoreDataSource.getUserProfile: profileImageUrl: ${data?.get("profileImageUrl")}")
                data
            } else {
                println("FirestoreDataSource.getUserProfile: Documento NO existe en Firestore para usuario: $userId")
                null
            }
        } catch (e: Exception) {
            println("FirestoreDataSource.getUserProfile: ERROR al obtener perfil: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    override
    suspend fun createUserDocument(
        userId: String,
        email: String,
        fullName: String?,
        favoriteGenre: String?,
        birthYear: String?
    ) {
        println("FirestoreDataSource.createUserDocument: Creando documento para usuario: $userId")
        
        val userData = mutableMapOf<String, Any>(
            "email" to email,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )
        
        fullName?.let { userData["fullName"] = it }
        favoriteGenre?.let { userData["favoriteGenre"] = it }
        birthYear?.let { userData["birthYear"] = it }
        
        try {
            firestoreService.collection("users")
                .document(userId)
                .set(userData)
                .await()
            println("FirestoreDataSource.createUserDocument: Documento creado exitosamente")
        } catch (e: Exception) {
            println("FirestoreDataSource.createUserDocument: ERROR al crear documento: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun deleteUserDocument(userId: String) {
        try {
            firestoreService.collection("users")
                .document(userId)
                .delete()
                .await()
            println("FirestoreDataSource.deleteUserDocument: Documento eliminado exitosamente")
        } catch (e: Exception) {
            println("FirestoreDataSource.deleteUserDocument: ERROR al eliminar documento: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
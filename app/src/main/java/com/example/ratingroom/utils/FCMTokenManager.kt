package com.example.ratingroom.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "FCMTokenManager"
    }

    /**
     * 🔑 Obtiene y guarda el token FCM del dispositivo
     */
    suspend fun refreshFCMToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ FCM Token obtenido: $token")
            
            saveTokenToFirestore(token)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo FCM Token: ${e.message}", e)
        }
    }

    /**
     * 💾 Guarda el token en Firestore para el usuario actual
     */
    private suspend fun saveTokenToFirestore(token: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "fcmToken" to token,
                            "fcmTokenUpdatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

                Log.d(TAG, "✅ Token FCM guardado en Firestore")
            } else {
                Log.w(TAG, "⚠️ Usuario no autenticado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando token: ${e.message}", e)
        }
    }

    /**
     * 🗑️ Elimina el token FCM cuando el usuario cierra sesión
     */
    suspend fun clearFCMToken() {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users")
                    .document(userId)
                    .update("fcmToken", null)
                    .await()

                Log.d(TAG, "✅ Token FCM eliminado de Firestore")
            }

            // Eliminar token del dispositivo
            FirebaseMessaging.getInstance().deleteToken().await()
            Log.d(TAG, "✅ Token FCM eliminado del dispositivo")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error eliminando token: ${e.message}", e)
        }
    }

    /**
     * ✅ Verifica si tiene permiso de notificaciones (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se necesita permiso explícito
        }
    }
}

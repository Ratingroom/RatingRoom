package com.example.ratingroom.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ratingroom.MainActivity
import com.example.ratingroom.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "ratingroom_notifications"
        private const val CHANNEL_NAME = "RatingRoom Notifications"
    }

    /**
     * 🔔 Se llama cuando llega una notificación mientras la app está en FOREGROUND
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📩 Mensaje recibido de: ${message.from}")

        // Datos personalizados
        message.data.let { data ->
            Log.d(TAG, "📦 Payload data: $data")
            val type = data["type"] ?: "default"
            val title = data["title"] ?: "RatingRoom"
            val body = data["body"] ?: ""
            val relatedItemId = data["relatedItemId"] ?: ""

            showNotification(title, body, type, relatedItemId)
        }

        // Notificación visual (si viene del servidor)
        message.notification?.let { notification ->
            Log.d(TAG, "💬 Notification: ${notification.title} - ${notification.body}")
            showNotification(
                title = notification.title ?: "RatingRoom",
                message = notification.body ?: "",
                type = "default",
                relatedItemId = ""
            )
        }
    }

    /**
     * 🆕 Se llama cuando se genera o actualiza el token FCM
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 Nuevo FCM Token: $token")

        // Guardar el token en Firestore para el usuario actual
        saveTokenToFirestore(token)
    }

    /**
     * 💾 Guarda el token FCM en Firestore
     */
    private fun saveTokenToFirestore(token: String) {
        serviceScope.launch {
            try {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    firestore.collection("users")
                        .document(userId)
                        .update("fcmToken", token)
                        .await()

                    Log.d(TAG, "✅ Token FCM guardado en Firestore para usuario: $userId")
                } else {
                    Log.w(TAG, "⚠️ Usuario no autenticado, no se puede guardar token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error guardando token FCM: ${e.message}", e)
            }
        }
    }

    /**
     * 🔔 Muestra una notificación local
     */
    private fun showNotification(
        title: String,
        message: String,
        type: String,
        relatedItemId: String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            putExtra("related_item_id", relatedItemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logoratingroom) // Asegúrate de tener este icono
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de seguidores, likes y comentarios"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}

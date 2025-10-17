# 🔔 Configuración de Notificaciones Firebase (FCM) en RatingRoom

Este documento explica cómo funcionan las notificaciones push en RatingRoom usando **Firebase Cloud Messaging (FCM)**.

## 📋 Tabla de Contenidos

1. [Configuración Inicial](#configuración-inicial)
2. [Arquitectura](#arquitectura)
3. [Componentes Implementados](#componentes-implementados)
4. [Cloud Functions](#cloud-functions)
5. [Testing](#testing)
6. [Troubleshooting](#troubleshooting)

---

## 🚀 Configuración Inicial

### 1. Dependencias (Ya agregadas)

En `app/build.gradle.kts`:
```kotlin
implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")
```

### 2. Permisos en AndroidManifest.xml (Ya agregados)

```xml
<!-- Para Android 13+ -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### 3. Servicio FCM Registrado (Ya configurado)

En `AndroidManifest.xml`:
```xml
<service
    android:name=".service.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                    USUARIO A                                 │
│  (Sigue a Usuario B / Da like a reseña)                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              FIRESTORE TRIGGER                               │
│  (Cloud Function detecta cambio en base de datos)           │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│         CLOUD FUNCTION (Firebase Functions)                  │
│  1. Crea notificación en Firestore                         │
│  2. Obtiene FCM Token del Usuario B                        │
│  3. Envía notificación push via FCM                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│              FIREBASE CLOUD MESSAGING                        │
│  (Enruta notificación al dispositivo)                      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│        USUARIO B (Dispositivo Android)                       │
│  - App en FOREGROUND → MyFirebaseMessagingService          │
│  - App en BACKGROUND → Sistema Android muestra notif       │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Componentes Implementados

### 1. `MyFirebaseMessagingService.kt`

**Ubicación:** `app/src/main/java/com/example/ratingroom/service/`

**Responsabilidades:**
- ✅ Recibe notificaciones push mientras la app está en **foreground**
- ✅ Muestra notificaciones locales con sonido y vibración
- ✅ Maneja actualizaciones del token FCM
- ✅ Guarda el token en Firestore automáticamente

**Métodos clave:**
```kotlin
override fun onMessageReceived(message: RemoteMessage)
override fun onNewToken(token: String)
```

---

### 2. `FCMTokenManager.kt`

**Ubicación:** `app/src/main/java/com/example/ratingroom/utils/`

**Responsabilidades:**
- ✅ Obtiene el token FCM del dispositivo
- ✅ Guarda el token en Firestore (`users/{userId}/fcmToken`)
- ✅ Elimina el token al cerrar sesión
- ✅ Verifica permisos de notificaciones

**Métodos principales:**
```kotlin
suspend fun refreshFCMToken()
suspend fun clearFCMToken()
fun hasNotificationPermission(context: Context): Boolean
```

---

### 3. Integración en `MainActivity.kt`

**Función:** Solicita permiso de notificaciones (Android 13+) y obtiene token al iniciar la app.

```kotlin
private fun requestNotificationPermissionAndGetToken() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Solicitar permiso POST_NOTIFICATIONS
    }
    lifecycleScope.launch {
        fcmTokenManager.refreshFCMToken()
    }
}
```

---

### 4. Integración en `LoginViewModel.kt`

**Función:** Obtiene y guarda token FCM después de un login exitoso.

```kotlin
authRepository.signIn(email, password)
    .onSuccess { user ->
        fcmTokenManager.refreshFCMToken() // 🔔 Obtener token
        onSuccess()
    }
```

---

### 5. Integración en `AuthRepository.kt`

**Función:** Limpia el token FCM al cerrar sesión.

```kotlin
suspend fun signOut() {
    fcmTokenManager.clearFCMToken() // 🗑️ Eliminar token
    authRemoteDataSource.signOut()
}
```

---

## ☁️ Cloud Functions

### Funciones Desplegadas

#### 1. `sendFollowNotification`

**Trigger:** `users/{userId}/followers/{followerId}` (onCreate)

**Flujo:**
1. Obtiene datos del seguidor (nombre, foto)
2. Obtiene token FCM del usuario seguido
3. Crea notificación en Firestore
4. Envía notificación push

**Payload:**
```json
{
  "notification": {
    "title": "🎬 Nuevo seguidor",
    "body": "Juan comenzó a seguirte"
  },
  "data": {
    "type": "follow",
    "fromUserId": "abc123",
    "relatedItemId": "abc123"
  }
}
```

---

#### 2. `sendLikeNotification`

**Trigger:** `reviews/{reviewId}/likes/{userId}` (onCreate)

**Flujo:**
1. Obtiene datos de la reseña y del usuario que dio like
2. Verifica que no sea el mismo autor
3. Obtiene token FCM del autor de la reseña
4. Crea notificación en Firestore
5. Envía notificación push

**Payload:**
```json
{
  "notification": {
    "title": "❤️ Nueva reacción",
    "body": "A María le gustó tu reseña de \"Inception\""
  },
  "data": {
    "type": "like",
    "fromUserId": "xyz789",
    "relatedItemId": "reviewId123"
  }
}
```

---

### Despliegue de Cloud Functions

```bash
cd /media/juegos/CODES/DESMOVIL/CLOUD-FUNCTIONS/functions

# Instalar dependencias
npm install

# Desplegar todas las funciones
firebase deploy --only functions

# O desplegar funciones específicas
firebase deploy --only functions:sendFollowNotification,functions:sendLikeNotification
```

---

## 🧪 Testing

### 1. Testing Local (Emuladores)

```bash
# En el directorio de functions
firebase emulators:start --only functions,firestore
```

### 2. Testing Manual (Consola Firebase)

1. Ve a **Firebase Console** → **Cloud Messaging**
2. Clic en **"Send test message"**
3. Pega el token FCM de tu dispositivo
4. Envía mensaje de prueba

**Para obtener tu token:**
- Revisa los logs de Android Studio al iniciar la app
- Busca: `🔑 FCM Token obtenido: ...`

### 3. Testing End-to-End

**Escenario 1: Seguir usuario**
```
1. Usuario A sigue a Usuario B
2. Usuario B debe recibir notificación: "Usuario A comenzó a seguirte"
```

**Escenario 2: Like en reseña**
```
1. Usuario A da like a reseña de Usuario B
2. Usuario B debe recibir: "A Usuario A le gustó tu reseña de [Película]"
```

---

## 🐛 Troubleshooting

### ❌ No recibo notificaciones

**Posibles causas:**

1. **Token FCM no guardado en Firestore**
   ```kotlin
   // Verificar en logs:
   ✅ Token FCM guardado en Firestore
   ```

2. **Permiso de notificaciones denegado (Android 13+)**
   ```kotlin
   // En MainActivity, verifica:
   fcmTokenManager.hasNotificationPermission(this)
   ```

3. **Cloud Function no desplegada**
   ```bash
   firebase deploy --only functions
   ```

4. **Token expirado**
   - Los tokens FCM pueden expirar
   - La función `onNewToken()` se encarga de actualizarlos automáticamente

---

### ❌ Notificaciones solo en foreground

**Solución:** Las notificaciones en **background** son manejadas automáticamente por el sistema Android si incluyes el campo `notification` en el payload FCM.

**Estructura correcta:**
```javascript
const message = {
  token: fcmToken,
  notification: {  // ✅ Necesario para background
    title: "Título",
    body: "Mensaje"
  },
  data: {
    type: "follow",
    // ... otros datos
  }
};
```

---

### ❌ Error: "App is missing...google-services.json"

**Solución:**
1. Descarga `google-services.json` desde Firebase Console
2. Colócalo en `app/google-services.json`
3. Rebuild el proyecto

---

## 📊 Estructura de Datos en Firestore

### Token FCM

```
users/
  └── {userId}/
      ├── fullName: "Juan Pérez"
      ├── email: "juan@example.com"
      ├── fcmToken: "dXKzY3p5..." ← Token FCM
      └── fcmTokenUpdatedAt: 1697500000000
```

### Notificaciones

```
users/
  └── {userId}/
      └── notifications/
          └── {notificationId}/
              ├── type: "follow" | "like"
              ├── fromUserId: "abc123"
              ├── fromUserName: "María"
              ├── fromUserImage: "https://..."
              ├── message: "María comenzó a seguirte"
              ├── timestamp: Timestamp
              ├── isRead: false
              └── relatedItemId: "abc123"
```

---

## 📝 Notas Importantes

1. **Permisos Android 13+:** Siempre solicita `POST_NOTIFICATIONS` en runtime.

2. **Token Refresh:** Los tokens FCM pueden cambiar. `onNewToken()` se encarga automáticamente.

3. **Límites FCM:**
   - **Gratís:** 10 mensajes/segundo
   - **Blaze:** Sin límite

4. **Notificaciones en China:** FCM no funciona en China. Considera alternativas como Huawei Push Kit.

5. **Batería:** Las notificaciones push consumen menos batería que polling manual.

---

## 🎯 Próximos Pasos

- [ ] Implementar notificaciones para comentarios
- [ ] Agregar imágenes en notificaciones (BigPicture style)
- [ ] Implementar agrupación de notificaciones
- [ ] Agregar acciones en notificaciones (Responder, Ver)
- [ ] Analytics de notificaciones abiertas

---

## 📚 Recursos

- [Firebase Cloud Messaging Docs](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [Firebase Functions Docs](https://firebase.google.com/docs/functions)

---

**Última actualización:** 17 de octubre de 2025  
**Versión:** 1.0.0

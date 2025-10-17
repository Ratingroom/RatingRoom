# 🔔 Guía Rápida: Notificaciones Firebase en RatingRoom

## ✅ ¿Qué se implementó?

### 📱 En la App Android (Kotlin)

1. **MyFirebaseMessagingService** - Recibe y muestra notificaciones
2. **FCMTokenManager** - Gestiona tokens FCM
3. **MainActivity** - Solicita permisos de notificaciones
4. **LoginViewModel** - Obtiene token al hacer login
5. **AuthRepository** - Limpia token al cerrar sesión

### ☁️ En Cloud Functions (JavaScript)

1. **sendFollowNotification** - Notifica cuando te siguen
2. **sendLikeNotification** - Notifica cuando dan like a tu reseña

---

## 🚀 Cómo usar

### Paso 1: Build & Run la app

```bash
cd /media/juegos/CODES/DESMOVIL/RatingRoom
./gradlew assembleDebug
```

### Paso 2: Desplegar Cloud Functions

```bash
cd /media/juegos/CODES/DESMOVIL/CLOUD-FUNCTIONS
firebase deploy --only functions:sendFollowNotification,functions:sendLikeNotification
```

### Paso 3: Probar

1. **Login en la app** → Se obtiene automáticamente el token FCM
2. **Usuario A sigue a Usuario B** → Usuario B recibe notificación 🔔
3. **Usuario A da like a reseña** → Autor recibe notificación ❤️

---

## 📊 Flujo de Notificaciones

```
┌──────────────┐
│   Usuario A  │
│ (Sigue/Like) │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│    Firestore     │ ← Trigger
│  (users/reviews) │
└──────┬───────────┘
       │
       ▼
┌─────────────────────────┐
│   Cloud Function        │
│ 1. Crea notificación    │
│ 2. Obtiene FCM Token    │
│ 3. Envía Push (FCM)     │
└──────┬──────────────────┘
       │
       ▼
┌──────────────────┐
│   Usuario B      │
│ Recibe 🔔        │
└──────────────────┘
```

---

## 🎯 Archivos Clave

| Archivo | Propósito |
|---------|-----------|
| `MyFirebaseMessagingService.kt` | Maneja notificaciones entrantes |
| `FCMTokenManager.kt` | Gestiona tokens FCM |
| `MainActivity.kt` | Pide permisos Android 13+ |
| `CLOUD-FUNCTIONS/functions/index.js` | Envía notificaciones push |

---

## 🔍 Ver Logs

**Android Studio:**
```
Buscar: "FCM" o "🔔" en Logcat
```

**Cloud Functions:**
```bash
firebase functions:log
```

---

## 🐛 Problemas Comunes

### ❌ No recibo notificaciones

**Checklist:**
- [ ] Permiso `POST_NOTIFICATIONS` concedido (Android 13+)
- [ ] Token FCM guardado en Firestore (`users/{userId}/fcmToken`)
- [ ] Cloud Functions desplegadas
- [ ] App en foreground o background (no force-stop)

### ❌ Error al compilar

**Solución:**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

---

## 📖 Documentación Completa

Ver: `/docs/NOTIFICACIONES_FIREBASE.md`

---

## 🎉 ¡Listo para usar!

Las notificaciones push ya están completamente configuradas en RatingRoom. Solo necesitas:

1. ✅ Compilar y ejecutar la app
2. ✅ Desplegar las Cloud Functions
3. ✅ ¡Probar!

**Siguiente:** Implementar notificaciones para comentarios y otras acciones 🚀

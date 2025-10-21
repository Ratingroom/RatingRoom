# 🔥 Configuración de Firebase para las Correcciones

## 📌 Índices Compuestos Requeridos

### Crear en Firebase Console → Firestore Database → Indexes:

### 1. **Índice para Reviews por Película**
```
Collection: reviews
Fields indexed:
  - movieId: Ascending
  - createdAt: Descending
Query scope: Collection
```

**Comando CLI (alternativa):**
```bash
firebase firestore:indexes:add reviews --field movieId:asc --field createdAt:desc
```

### 2. **Índice para Reviews por Usuario**
```
Collection: reviews
Fields indexed:
  - userId: Ascending
  - createdAt: Descending
Query scope: Collection
```

**Comando CLI (alternativa):**
```bash
firebase firestore:indexes:add reviews --field userId:asc --field createdAt:desc
```

### 3. **Índice para Notificaciones**
```
Collection: users/{userId}/notifications
Fields indexed:
  - createdAt: Descending
  - seen: Ascending
Query scope: Collection group
```

---

## 🔐 Reglas de Seguridad Actualizadas

### `firestore.rules`

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========== USUARIOS ==========
    match /users/{userId} {
      // Leer: público
      allow read: if true;
      
      // Crear: solo el propio usuario
      allow create: if request.auth != null && request.auth.uid == userId;
      
      // Actualizar: solo el propio usuario
      allow update: if request.auth != null && request.auth.uid == userId;
      
      // Subcolecciones de usuarios
      match /notifications/{notificationId} {
        // Solo el dueño puede leer y actualizar sus notificaciones
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /likes/{likeId} {
        // Solo el dueño puede leer y modificar sus likes
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
      
      match /followers/{followerId} {
        // Leer: público
        allow read: if true;
        // Escribir: solo usuarios autenticados
        allow write: if request.auth != null;
      }
      
      match /following/{followingId} {
        // Leer: público
        allow read: if true;
        // Escribir: solo el propio usuario
        allow write: if request.auth != null && request.auth.uid == userId;
      }
    }
    
    // ========== REVIEWS (COLECCIÓN PRINCIPAL) ==========
    match /reviews/{reviewId} {
      // Leer: público
      allow read: if true;
      
      // Crear: usuario autenticado y el userId en el documento coincide
      allow create: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
      
      // Actualizar: solo el autor puede actualizar
      allow update: if request.auth != null && 
                      resource.data.userId == request.auth.uid;
      
      // Eliminar: solo el autor puede eliminar
      allow delete: if request.auth != null && 
                      resource.data.userId == request.auth.uid;
      
      // Subcolección de likes dentro de reviews
      match /likes/{likeUserId} {
        // Leer: público
        allow read: if true;
        
        // Escribir: solo el usuario que da el like
        allow write: if request.auth != null && 
                       request.auth.uid == likeUserId;
      }
    }
    
    // ========== PELÍCULAS ==========
    match /movies/{movieId} {
      // Leer: público
      allow read: if true;
      
      // Escribir: solo administradores (requiere claim personalizado)
      allow write: if request.auth != null && 
                     request.auth.token.admin == true;
    }
  }
}
```

---

## 🗂️ Estructura de Datos Firebase

### Colección `reviews` (ÚNICA FUENTE DE VERDAD)

```javascript
reviews/{reviewId}
  ├─ id: string
  ├─ userId: string          // UID de Firebase del autor
  ├─ movieId: number
  ├─ rating: number          // 1-5
  ├─ text: string
  ├─ likes: number           // Contador
  ├─ createdAt: timestamp
  ├─ updatedAt: timestamp
  ├─ userName: string        // Desnormalizado
  ├─ userImageUrl: string    // Desnormalizado
  └─ likes/{userId}          // Subcolección para tracking
      └─ timestamp: timestamp
```

### Colección `users`

```javascript
users/{userId}
  ├─ email: string
  ├─ fullName: string
  ├─ biography: string
  ├─ profileImageUrl: string
  ├─ createdAt: timestamp
  ├─ updatedAt: timestamp
  ├─ notifications/{notificationId}
  │   ├─ type: "like" | "follow"
  │   ├─ actorUserId: string
  │   ├─ actorName: string
  │   ├─ reviewId: string (opcional)
  │   ├─ movieId: number (opcional)
  │   ├─ createdAt: timestamp
  │   └─ seen: boolean
  ├─ likes/{reviewId}        // Espejo para queries rápidas
  │   └─ timestamp: timestamp
  ├─ followers/{followerId}
  │   └─ timestamp: timestamp
  └─ following/{followingId}
      └─ timestamp: timestamp
```

### Colección `movies`

```javascript
movies/{movieId}
  ├─ id: number
  ├─ title: string
  ├─ description: string
  ├─ year: string
  ├─ genre: string
  ├─ director: string
  ├─ duration: string
  ├─ imageUrl: string
  ├─ rating: number
  └─ reviews: number         // Contador de reviews
```

---

## 📜 Script de Migración de Datos

### Opción 1: Cloud Function (Recomendado)

Crea este archivo como `functions/migrateReviews.js`:

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.migrateReviewsToMainCollection = functions.https.onRequest(async (req, res) => {
  const db = admin.firestore();
  
  try {
    // 1. Obtener todas las reviews de users/{userId}/reviews
    const usersSnapshot = await db.collection('users').get();
    let migratedCount = 0;
    let skippedCount = 0;
    
    for (const userDoc of usersSnapshot.docs) {
      const userReviews = await db.collection('users')
        .doc(userDoc.id)
        .collection('reviews')
        .get();
      
      for (const reviewDoc of userReviews.docs) {
        const reviewData = reviewDoc.data();
        const reviewId = reviewDoc.id;
        
        // Verificar si ya existe en la colección principal
        const mainReviewDoc = await db.collection('reviews').doc(reviewId).get();
        
        if (!mainReviewDoc.exists()) {
          // Copiar a la colección principal
          await db.collection('reviews').doc(reviewId).set({
            ...reviewData,
            id: reviewId,
            userId: userDoc.id,
            // Asegurar campos requeridos
            likes: reviewData.likes || 0,
            createdAt: reviewData.createdAt || admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: reviewData.updatedAt || admin.firestore.FieldValue.serverTimestamp()
          });
          migratedCount++;
        } else {
          skippedCount++;
        }
      }
    }
    
    // 2. Hacer lo mismo para movies/{movieId}/reviews
    const moviesSnapshot = await db.collection('movies').get();
    
    for (const movieDoc of moviesSnapshot.docs) {
      const movieReviews = await db.collection('movies')
        .doc(movieDoc.id)
        .collection('reviews')
        .get();
      
      for (const reviewDoc of movieReviews.docs) {
        const reviewData = reviewDoc.data();
        const reviewId = reviewDoc.id;
        
        const mainReviewDoc = await db.collection('reviews').doc(reviewId).get();
        
        if (!mainReviewDoc.exists()) {
          await db.collection('reviews').doc(reviewId).set({
            ...reviewData,
            id: reviewId,
            movieId: parseInt(movieDoc.id),
            likes: reviewData.likes || 0,
            createdAt: reviewData.createdAt || admin.firestore.FieldValue.serverTimestamp(),
            updatedAt: reviewData.updatedAt || admin.firestore.FieldValue.serverTimestamp()
          });
          migratedCount++;
        } else {
          skippedCount++;
        }
      }
    }
    
    res.json({
      success: true,
      migratedCount,
      skippedCount,
      message: `Migración completada. ${migratedCount} reviews migradas, ${skippedCount} ya existían.`
    });
    
  } catch (error) {
    console.error('Error en migración:', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// Cloud Function para limpiar subcolecciones antiguas (USAR CON PRECAUCIÓN)
exports.cleanupOldReviewSubcollections = functions.https.onRequest(async (req, res) => {
  const db = admin.firestore();
  const CONFIRM_TOKEN = 'CONFIRM_DELETE_OLD_REVIEWS';
  
  // Requiere token de confirmación en query param
  if (req.query.confirm !== CONFIRM_TOKEN) {
    return res.status(403).json({ 
      error: 'Se requiere token de confirmación. Pasa ?confirm=CONFIRM_DELETE_OLD_REVIEWS' 
    });
  }
  
  try {
    let deletedCount = 0;
    const batch = db.batch();
    let batchCount = 0;
    const MAX_BATCH = 500;
    
    // Eliminar de users/{userId}/reviews
    const usersSnapshot = await db.collection('users').get();
    
    for (const userDoc of usersSnapshot.docs) {
      const userReviews = await db.collection('users')
        .doc(userDoc.id)
        .collection('reviews')
        .get();
      
      for (const reviewDoc of userReviews.docs) {
        batch.delete(reviewDoc.ref);
        deletedCount++;
        batchCount++;
        
        if (batchCount >= MAX_BATCH) {
          await batch.commit();
          batchCount = 0;
        }
      }
    }
    
    // Commit final
    if (batchCount > 0) {
      await batch.commit();
    }
    
    res.json({
      success: true,
      deletedCount,
      message: `${deletedCount} documentos eliminados de subcolecciones antiguas.`
    });
    
  } catch (error) {
    console.error('Error limpiando subcolecciones:', error);
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});
```

### Opción 2: Script de Node.js Local

```javascript
// migrateReviews.js
const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function migrateReviews() {
  console.log('🚀 Iniciando migración de reviews...');
  
  // Tu lógica aquí (similar a la Cloud Function)
  
  console.log('✅ Migración completada');
  process.exit(0);
}

migrateReviews().catch(console.error);
```

**Ejecutar:**
```bash
node migrateReviews.js
```

---

## ✅ Checklist de Despliegue

- [ ] Crear índices compuestos en Firestore
- [ ] Actualizar reglas de seguridad de Firestore
- [ ] Ejecutar script de migración de datos
- [ ] Verificar que las reviews se lean correctamente desde la app
- [ ] Probar creación de nuevas reviews
- [ ] Probar sistema de likes
- [ ] Probar notificaciones
- [ ] Verificar actualización en tiempo real
- [ ] (Opcional) Limpiar subcolecciones antiguas después de verificar

---

## 🆘 Troubleshooting

### Error: "Missing or insufficient permissions"
**Solución:** Verificar reglas de Firestore y que el usuario esté autenticado.

### Error: "The query requires an index"
**Solución:** Firebase te dará un link para crear el índice. Haz clic y espera 2-3 minutos.

### Las reviews no se actualizan en tiempo real
**Solución:** Verificar que estés usando `observeReviewsByMovie()` o `observeReviewsByUser()` en el ViewModel.

### Los likes no se guardan
**Solución:** Verificar que las reglas de Firestore permitan escribir en `reviews/{reviewId}/likes/{userId}`.

---

**Última actualización:** 20 de octubre de 2025

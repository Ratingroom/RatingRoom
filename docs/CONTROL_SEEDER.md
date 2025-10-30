# 🎲 Control del Seeder de Datos de Prueba

## 📋 Descripción

El proyecto incluye un **seeder de datos de prueba** (`FakeDbSeeder`) que genera automáticamente:
- ✅ 20 usuarios falsos con nombres, emails y géneros favoritos
- ✅ Relaciones de seguidores/seguidos entre usuarios
- ✅ Reseñas aleatorias de películas

Este seeder **solo se ejecuta en modo DEBUG** y está controlado por una variable de configuración.

---

## 🔧 Configuración

### Variable de Control

En `app/build.gradle.kts` encontrarás:

```kotlin
buildTypes {
    debug {
        isDebuggable = true
        // Variable para controlar el seeder de datos de prueba
        buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
    }
    release {
        buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
    }
}
```

### Estados Posibles

| Valor | Descripción |
|-------|-------------|
| `"false"` | **Deshabilitado** - No genera datos (por defecto) |
| `"true"` | **Habilitado** - Genera datos de prueba al iniciar la app |

---

## 🚀 Cómo Habilitar el Seeder

### Opción 1: Editar build.gradle.kts (Recomendado)

1. Abre `app/build.gradle.kts`
2. Cambia el valor en la sección `debug`:
   ```kotlin
   debug {
       isDebuggable = true
       buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "true")  // ← Cambiar a true
   }
   ```
3. Sincroniza Gradle
4. Limpia y reinstala la app:
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```

### Opción 2: Crear Variant de Build (Avanzado)

Puedes crear un build variant específico para testing:

```kotlin
buildTypes {
    debug {
        isDebuggable = true
        buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
    }
    
    // Nuevo variant para testing con datos
    getByName("debug") {
        initWith(getByName("debug"))
    }
}

flavorDimensions += "data"
productFlavors {
    create("withTestData") {
        dimension = "data"
        buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "true")
    }
    create("noTestData") {
        dimension = "data"
        buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
    }
}
```

Luego podrás elegir el variant `withTestDataDebug` en Android Studio.

---

## 🔍 Verificación

### Ver si el Seeder está Activo

Revisa los logs en Logcat:

```bash
# Si está DESHABILITADO, verás:
adb logcat | grep MainActivity
# ℹ️ Seeder de datos deshabilitado (ENABLE_TEST_DATA_SEEDER=false)

# Si está HABILITADO, verás:
# ✅ Seeder de datos de prueba ejecutado
```

### Ver el Seeder en Acción

```bash
adb logcat | grep FakeDbSeeder
# Seed ya ejecutado; saltando.
# O: Seed completado correctamente.
```

---

## 🛑 Cómo Deshabilitar el Seeder

### Método 1: Cambiar BuildConfig (Recomendado)

1. En `app/build.gradle.kts`, cambia a `"false"`:
   ```kotlin
   buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
   ```
2. Sincroniza Gradle
3. Desinstala y reinstala la app

### Método 2: Limpiar la Marca del Seeder

El seeder usa una marca en Firestore (`/meta/seed_debug_v1`) para evitar duplicados. Si quieres que se ejecute de nuevo:

1. Ve a Firebase Console
2. Abre Firestore Database
3. Elimina el documento `/meta/seed_debug_v1`
4. Vuelve a ejecutar la app

---

## 📊 Datos Generados

### Usuarios
```javascript
// Colección: users
{
  "email": "juan.garcia@mail.com",
  "fullName": "Juan García",
  "favoriteGenre": "Acción",
  "followersCount": 5,
  "followingCount": 3,
  "createdAt": 1234567890,
  "updatedAt": 1234567890
}
```

### Relaciones
```javascript
// Subcolección: users/{uid}/following
{
  "timestamp": serverTimestamp()
}

// Subcolección: users/{uid}/followers
{
  "timestamp": serverTimestamp()
}
```

### Reseñas
```javascript
// Colección: reviews
{
  "userId": "user-uuid",
  "movieId": 15,
  "rating": 4,
  "text": "Muy buena película (4/5)",
  "createdAt": serverTimestamp()
}

// También se replica en:
// - users/{uid}/reviews/{reviewId}
// - movies/{movieId}/reviews/{reviewId}
```

---

## ⚠️ Consideraciones Importantes

### 1. **Idempotencia**
El seeder solo se ejecuta **una vez** por instalación. Usa la marca `/meta/seed_debug_v1` en Firestore.

### 2. **Solo en Debug**
El código del seeder está en `app/src/debug/java/`, por lo que:
- ✅ Se incluye en builds Debug
- ❌ **NO** se incluye en builds Release
- ✅ Seguro para producción

### 3. **Dependencia Faker**
Usa `kotlin-faker` solo en debug:
```kotlin
debugImplementation("io.github.serpro69:kotlin-faker:1.15.0")
```

### 4. **Impacto en Firestore**
Genera aproximadamente:
- 20 usuarios
- 40-80 relaciones de seguimiento
- 40-100 reseñas

Esto puede consumir tu cuota gratuita de Firestore si lo ejecutas muchas veces.

---

## 🧹 Limpiar Datos de Prueba

### Opción 1: Desde Firebase Console

1. Ve a Firestore Database
2. Elimina las colecciones:
   - `/users` (y todas las subcolecciones)
   - `/reviews`
   - `/meta`

### Opción 2: Script de Limpieza

Puedes crear un script para limpiar:

```kotlin
// En tu debug tools
fun cleanTestData(firestore: FirebaseFirestore) {
    // Eliminar usuarios de prueba
    firestore.collection("users")
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
    
    // Eliminar reviews
    firestore.collection("reviews")
        .get()
        .await()
        .documents
        .forEach { it.reference.delete().await() }
    
    // Eliminar marca
    firestore.collection("meta")
        .document("seed_debug_v1")
        .delete()
        .await()
}
```

---

## 🐛 Troubleshooting

### Problema: El seeder se ejecuta cada vez que abro la app

**Causa**: La marca `/meta/seed_debug_v1` no se está guardando correctamente.

**Solución**:
1. Verifica que `ENABLE_TEST_DATA_SEEDER = true`
2. Revisa logs para ver si hay errores
3. Verifica permisos de escritura en Firestore Rules

### Problema: El seeder no genera datos

**Causas posibles**:
1. `ENABLE_TEST_DATA_SEEDER = false` (por defecto)
2. La marca ya existe en Firestore
3. Error en permisos de Firestore

**Soluciones**:
1. Habilita el seeder en `build.gradle.kts`
2. Elimina `/meta/seed_debug_v1` de Firestore
3. Verifica Firestore Rules permiten escritura

### Problema: Faker no funciona

**Síntoma**: Logs muestran "Faker no disponible, usando Random"

**Causa**: La librería `kotlin-faker` no está disponible

**Solución**: El seeder tiene un fallback, generará nombres genéricos como "Juan García"

---

## 📚 Referencias

- **Código del Seeder**: `app/src/debug/java/com/example/ratingroom/debug/FakeDbSeeder.kt`
- **Activación**: `app/src/main/java/com/example/ratingroom/MainActivity.kt`
- **Configuración**: `app/build.gradle.kts`
- **Kotlin Faker**: https://github.com/serpro69/kotlin-faker

---

## 🎯 Casos de Uso

### Desarrollo Local
```kotlin
// En build.gradle.kts
buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "true")
```
✅ Genera datos para probar la app sin usuarios reales

### Testing con Firebase CLI
```kotlin
// En build.gradle.kts
buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
```
✅ No interfiere con tus datos de prueba manuales

### CI/CD
```kotlin
// En build.gradle.kts (release)
buildConfigField("boolean", "ENABLE_TEST_DATA_SEEDER", "false")
```
✅ Nunca genera datos en producción

---

**Configuración actual:** `ENABLE_TEST_DATA_SEEDER = false` (Deshabilitado)

Para habilitar el seeder, cambia el valor a `true` en `app/build.gradle.kts`.

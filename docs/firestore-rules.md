# Reglas de seguridad de Cloud Firestore

Las reglas de Firestore de KRONO viven en `firestore.rules` (raíz del repo) y son las que están publicadas en el proyecto de Firebase `krono-5ef6e`. `firebase.json` y `.firebaserc` solo apuntan a ese archivo y a ese proyecto por defecto; ninguno contiene secretos.

## Qué permiten

Cada usuario autenticado lee y escribe únicamente su propio documento `users/{uid}` y todo lo que cuelga de él. Cualquier otra ruta se deniega por defecto.

Rutas que usa la app hoy:

- `users/{uid}`
- `users/{uid}/tasks/{taskId}`
- `users/{uid}/timer/active`
- `users/{uid}/timeSessions/{sessionId}`
- `users/{uid}/focusTemplates/{templateId}` (plantillas de enfoque, F-10; las cubre la regla de subcolecciones, no requirió cambiar `firestore.rules`)

El script `KRONO-IA/` usa el Admin SDK, que se salta las reglas.

## Cómo publicarlas

**Por la consola:** Firebase Console, proyecto `krono-5ef6e`, Firestore Database, pestaña *Reglas*. Pega el contenido de `firestore.rules` y pulsa *Publicar*.

**Con la CLI de Firebase:**

```
firebase login
firebase deploy --only firestore:rules
```

Se toma el proyecto de `.firebaserc`. La publicación es manual: el CI no despliega reglas.

## Mantenimiento

- Si el modelo de datos cambia de rutas (una colección nueva fuera de `users/{uid}`, por ejemplo), actualiza `firestore.rules` y esta lista en el mismo PR, y publica las reglas.
- No versiones secretos: `google-services.json` y las claves de cuentas de servicio están en `.gitignore` y nunca deben subirse.

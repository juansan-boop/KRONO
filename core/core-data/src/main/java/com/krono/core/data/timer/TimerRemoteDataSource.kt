package com.krono.core.data.timer

import kotlinx.coroutines.flow.Flow

/**
 * Fuente remota del cronómetro en Cloud Firestore (F-08). Gestiona el documento
 * `users/{userId}/timer/active` y la colección `users/{userId}/timeSessions`.
 */
interface TimerRemoteDataSource {

    /** Observa el documento del cronómetro en curso; emite `null` si no existe. */
    fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?>

    /** Lee una vez el documento del cronómetro en curso, o `null` si no existe. */
    suspend fun getActiveTimer(userId: String): Map<String, Any?>?

    /** Escribe el documento del cronómetro en curso. */
    fun saveActiveTimer(userId: String, timer: Map<String, Any?>)

    /** Indica si la tarea todavía existe (pudo eliminarse mientras corría el cronómetro). */
    suspend fun taskExists(userId: String, taskId: String): Boolean

    /**
     * En una sola escritura atómica: guarda la sesión, suma [durationMillis] a la tarea
     * (solo si [addToTask]) y elimina el cronómetro en curso.
     */
    fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
        addToTask: Boolean,
    )
}

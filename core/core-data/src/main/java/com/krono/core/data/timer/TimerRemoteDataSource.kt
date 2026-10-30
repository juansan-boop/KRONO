package com.krono.core.data.timer

import kotlinx.coroutines.flow.Flow

/**
 * Fuente remota del cronómetro en Cloud Firestore (F-08). Gestiona el documento
 * `users/{userId}/timer/active` y la colección `users/{userId}/timeSessions`.
 */
interface TimerRemoteDataSource {

    /** Observa el documento del cronómetro en curso; emite `null` si no existe. */
    fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?>

    /**
     * Lee una vez el documento del cronómetro en curso, o `null` si no existe. No espera al
     * servidor más de un límite corto: sin red o con red lenta lee de la caché local, y si
     * tampoco está ahí lanza la excepción `UNAVAILABLE` de Firestore.
     */
    suspend fun getActiveTimer(userId: String): Map<String, Any?>?

    /** Escribe el documento del cronómetro en curso; suspende hasta la confirmación del servidor. */
    suspend fun saveActiveTimer(userId: String, timer: Map<String, Any?>)

    /**
     * Finaliza el cronómetro con dos escrituras que se encolan una detrás de otra, sin esperar
     * entre ellas:
     * 1. Un lote que guarda la sesión (con su id como id del documento, así que repetirlo la
     *    reemplaza) y elimina el cronómetro en curso. No depende de la tarea, así que nunca se
     *    rechaza porque esta se haya borrado.
     * 2. El `increment` de [durationMillis] en el tiempo real de la tarea, que falla con
     *    `NOT_FOUND` si la tarea ya no existe sin afectar al lote 1.
     *
     * Suspende hasta la confirmación del servidor de ambas. Lanza el error del lote 1 si este
     * falla; devuelve `false` si solo falló el incremento porque la tarea no existe.
     */
    suspend fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
    ): Boolean
}

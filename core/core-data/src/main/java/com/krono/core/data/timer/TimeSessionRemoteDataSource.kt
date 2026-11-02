package com.krono.core.data.timer

import kotlinx.coroutines.flow.Flow

/**
 * Fuente remota de lectura de las sesiones de tiempo (colección `users/{userId}/timeSessions`,
 * Sección 6 del SPEC). Las escribe [TimerRemoteDataSource] al finalizar el cronómetro.
 */
interface TimeSessionRemoteDataSource {

    /**
     * Observa los documentos de sesión iniciados en `[fromMillis, toMillis)`, en milisegundos
     * desde la época Unix. Si el listener falla, el flujo termina con la excepción del SDK.
     */
    fun observeSessionsBetween(
        userId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<List<Map<String, Any?>>>
}

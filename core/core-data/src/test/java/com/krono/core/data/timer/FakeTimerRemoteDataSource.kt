package com.krono.core.data.timer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de prueba en memoria de [TimerRemoteDataSource] que imita la caché local de Firestore.
 *
 * Las escrituras se aplican al instante y, si hay [serverConfirmation], después esperan a que se
 * complete, como la confirmación del servidor (si nunca se completa, simula estar sin conexión).
 * Al finalizar, el lote de la sesión y el incremento de la tarea son escrituras separadas, como
 * en Firestore: si la tarea no existe, solo falla el incremento.
 */
class FakeTimerRemoteDataSource : TimerRemoteDataSource {

    /** Sesión y duración de cada finalización aplicada. */
    data class Finished(val session: Map<String, Any?>, val durationMillis: Long)

    /** Cronómetro en curso por usuario. */
    val timers = mutableMapOf<String, Map<String, Any?>>()

    /** Tareas que existen en el servidor; sumar a cualquier otra falla con `NOT_FOUND`. */
    val existingTasks = mutableSetOf<String>()

    /** Finalizaciones aplicadas, en orden. */
    val finished = mutableListOf<Finished>()

    /** Sesiones guardadas por id, como documentos de `timeSessions`. */
    val sessionsById = linkedMapOf<String, Map<String, Any?>>()

    /** Tiempo sumado a cada tarea con `increment`. */
    val incrementsByTask = mutableMapOf<String, Long>()

    /** Error con el que fallan las escrituras antes de aplicarse, como un error local. */
    var writeFailure: Exception? = null

    /** Error con el que falla la lectura del cronómetro en curso (por ejemplo, sin red y sin caché). */
    var readFailure: Exception? = null

    /** Confirmación del servidor que esperan las escrituras; `null` para confirmar al instante. */
    var serverConfirmation: CompletableDeferred<Unit>? = null

    private val timerFlow = MutableStateFlow<Map<String, Any?>?>(null)

    /** Precarga el cronómetro en curso de [userId]. */
    fun givenActiveTimer(userId: String, timer: Map<String, Any?>) {
        timers[userId] = timer
        timerFlow.value = timer
    }

    override fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?> = timerFlow

    override suspend fun getActiveTimer(userId: String): Map<String, Any?>? {
        readFailure?.let { throw it }
        return timers[userId]
    }

    override suspend fun saveActiveTimer(userId: String, timer: Map<String, Any?>) {
        writeFailure?.let { throw it }
        givenActiveTimer(userId, timer)
        serverConfirmation?.await()
    }

    override suspend fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
    ): Boolean {
        writeFailure?.let { throw it }
        sessionsById[session[TimeSessionDto.FIELD_ID] as String] = session
        timers.remove(userId)
        timerFlow.value = null
        val addedToTask = taskId in existingTasks
        if (addedToTask) incrementsByTask[taskId] = (incrementsByTask[taskId] ?: 0L) + durationMillis
        finished += Finished(session, durationMillis)
        serverConfirmation?.await()
        return addedToTask
    }
}

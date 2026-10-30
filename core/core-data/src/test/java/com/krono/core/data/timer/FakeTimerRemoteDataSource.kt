package com.krono.core.data.timer

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de prueba en memoria de [TimerRemoteDataSource] que imita la caché local de Firestore.
 *
 * Las escrituras se aplican al instante y, si hay [serverConfirmation], después esperan a que se
 * complete, como la confirmación del servidor (si nunca se completa, simula estar sin conexión).
 * El lote de [finishActiveTimer] es atómico: si falla no aplica nada, como un `WriteBatch`.
 */
class FakeTimerRemoteDataSource : TimerRemoteDataSource {

    /** Sesión, duración y destino de cada lote de finalización aplicado. */
    data class Finished(val session: Map<String, Any?>, val durationMillis: Long, val addToTask: Boolean)

    /** Cronómetro en curso por usuario. */
    val timers = mutableMapOf<String, Map<String, Any?>>()

    /** Tareas que existen en el servidor. */
    val existingTasks = mutableSetOf<String>()

    /**
     * Tareas que la lectura previa ve como existentes pero que se borraron antes del lote, así que
     * el `update` del lote falla con `NOT_FOUND`.
     */
    val tasksDeletedAfterRead = mutableSetOf<String>()

    /** Lotes de finalización aplicados, en orden. */
    val finished = mutableListOf<Finished>()

    /** Sesiones guardadas por id, como documentos de `timeSessions`. */
    val sessionsById = linkedMapOf<String, Map<String, Any?>>()

    /** Tiempo sumado a cada tarea con `increment`. */
    val incrementsByTask = mutableMapOf<String, Long>()

    /** Error con el que fallan las escrituras antes de aplicarse, como un error local. */
    var writeFailure: Exception? = null

    /** Error con el que falla la lectura de existencia de la tarea (sin red y sin caché). */
    var taskExistsFailure: Exception? = null

    /** Error con el que falla la lectura del cronómetro en curso. */
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

    override suspend fun taskExists(userId: String, taskId: String): Boolean {
        taskExistsFailure?.let { throw it }
        return taskId in existingTasks || taskId in tasksDeletedAfterRead
    }

    override suspend fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
        addToTask: Boolean,
    ) {
        writeFailure?.let { throw it }
        if (addToTask && taskId !in existingTasks) {
            throw FirebaseFirestoreException("No existe la tarea", FirebaseFirestoreException.Code.NOT_FOUND)
        }
        sessionsById[session[TimeSessionDto.FIELD_ID] as String] = session
        if (addToTask) incrementsByTask[taskId] = (incrementsByTask[taskId] ?: 0L) + durationMillis
        timers.remove(userId)
        timerFlow.value = null
        finished += Finished(session, durationMillis, addToTask)
        serverConfirmation?.await()
    }
}

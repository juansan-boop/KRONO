package com.krono.feature.profile.data

import kotlin.time.Duration

/** Máximo de operaciones por lote de Firestore. */
internal const val MAX_BATCH_OPERATIONS = 500

/** Operaciones reservadas en el lote final: el cronómetro activo y el documento de perfil. */
internal const val FINAL_BATCH_RESERVED_OPERATIONS = 2

/** Máximo de tareas que caben en un lote, dejando sitio al cronómetro y al perfil del lote final. */
internal const val MAX_TASKS_PER_BATCH = MAX_BATCH_OPERATIONS - FINAL_BATCH_RESERVED_OPERATIONS

/**
 * Parte [taskRefs] en lotes de a lo sumo [MAX_TASKS_PER_BATCH] elementos.
 *
 * Siempre devuelve al menos un lote: el último es el lote final (que además lleva el cronómetro y
 * el perfil) y queda vacío cuando no hay tareas.
 */
internal fun <T> splitIntoPurgeBatches(taskRefs: List<T>): List<List<T>> =
    taskRefs.chunked(MAX_TASKS_PER_BATCH).ifEmpty { listOf(emptyList()) }

/**
 * Orquesta la purga de datos del usuario (F-21) con independencia de Firestore.
 *
 * Lee las tareas con [readTaskRefs], confirma los lotes previos con [commitTaskBatch] en orden y,
 * solo si todos terminaron, confirma el lote final con [commitFinalBatch] (cronómetro y perfil).
 * Cualquier fallo o tiempo agotado en un paso se propaga y los pasos siguientes no se ejecutan; por
 * eso, con más tareas de las que caben en un lote, un fallo intermedio deja tareas parcialmente
 * borradas con el perfil y el cronómetro intactos, y reintentar converge.
 *
 * Cada paso (la lectura y cada lote) tiene su propio límite [timeout], de modo que un usuario con
 * muchas tareas no agota un límite global compartido por todos los lotes en serie.
 *
 * @param T Referencia a una tarea en el origen remoto.
 * @throws com.krono.feature.profile.domain.RemoteTimeoutException si un paso excede [timeout].
 */
internal suspend fun <T> purgeInBatches(
    readTaskRefs: suspend () -> List<T>,
    commitTaskBatch: suspend (taskRefs: List<T>) -> Unit,
    commitFinalBatch: suspend (taskRefs: List<T>) -> Unit,
    timeout: Duration = REMOTE_OPERATION_TIMEOUT,
) {
    val batches = splitIntoPurgeBatches(withRemoteTimeout(timeout) { readTaskRefs() })
    batches.dropLast(1).forEach { batch ->
        withRemoteTimeout(timeout) { commitTaskBatch(batch) }
    }
    withRemoteTimeout(timeout) { commitFinalBatch(batches.last()) }
}

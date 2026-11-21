package com.krono.core.data.firestore

import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Espera máxima a la confirmación del servidor de una escritura de Firestore. Con red, Firestore
 * suele confirmar en menos de un segundo; sin red, el usuario solo espera este tiempo.
 */
internal val WRITE_CONFIRMATION_TIMEOUT: Duration = 2.seconds

/**
 * Resultado explícito de la confirmación de una escritura en Firestore.
 */
sealed interface WriteConfirmationResult {
    /** La escritura fue confirmada por el servidor remoto dentro del tiempo límite. */
    data object Confirmed : WriteConfirmationResult

    /** El tiempo límite expiró sin confirmación del servidor; la escritura quedó encolada en la caché local offline. */
    data object Queued : WriteConfirmationResult
}

/**
 * Ejecuta [write] (que suspende hasta la confirmación del servidor) esperando como mucho [timeout].
 *
 * Firestore aplica cada escritura al instante en su caché local y la encola para el servidor; sin
 * red la confirmación no llega hasta reconectar.
 *
 * @param timeout Tiempo máximo de espera a la confirmación del servidor.
 * @param write Operación de escritura suspendida.
 * @return [WriteConfirmationResult.Confirmed] si el servidor confirmó dentro del límite,
 * o [WriteConfirmationResult.Queued] si se agotó el tiempo. Errores reales se propagan como excepción.
 */
internal suspend fun awaitWriteConfirmation(
    timeout: Duration = WRITE_CONFIRMATION_TIMEOUT,
    write: suspend () -> Unit,
): WriteConfirmationResult {
    val completed = withTimeoutOrNull(timeout) {
        write()
        true
    }
    return if (completed == true) {
        WriteConfirmationResult.Confirmed
    } else {
        WriteConfirmationResult.Queued
    }
}

/**
 * Función de compatibilidad que delega en [awaitWriteConfirmation].
 */
internal suspend fun awaitConfirmationOrQueued(write: suspend () -> Unit): WriteConfirmationResult =
    awaitWriteConfirmation(write = write)

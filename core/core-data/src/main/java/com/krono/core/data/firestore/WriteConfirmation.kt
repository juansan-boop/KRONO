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
 * Ejecuta [write] (que suspende hasta la confirmación del servidor) esperando como mucho
 * [WRITE_CONFIRMATION_TIMEOUT].
 *
 * Firestore aplica cada escritura al instante en su caché local y la encola para el servidor; sin
 * red la confirmación no llega hasta reconectar. Por eso, si vence el límite, la escritura se da
 * por encolada y no es un error (F-02 CA-1). Los errores que llegan antes del límite (validación
 * local, permiso denegado, documento inexistente) se propagan como excepción.
 */
internal suspend fun awaitConfirmationOrQueued(write: suspend () -> Unit) {
    withTimeoutOrNull(WRITE_CONFIRMATION_TIMEOUT) { write() }
}

package com.krono.feature.profile.data

import com.krono.feature.profile.domain.NetworkUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Espera máxima a la confirmación del servidor de una escritura de perfil, preferencias o ajustes.
 *
 * Con red, Firestore confirma en menos de un segundo; sin red la confirmación no llega hasta
 * reconectar, así que el usuario solo espera este tiempo (mismo criterio que `core-data`).
 */
internal val PROFILE_WRITE_CONFIRMATION_TIMEOUT: Duration = 2.seconds

/**
 * Ejecuta [write] (que suspende hasta la confirmación del servidor) esperando como mucho [timeout].
 *
 * Firestore aplica cada escritura al instante en su caché local y la encola para el servidor, por lo
 * que, si vence el límite, la escritura se da por encolada y no es un error. Los errores que llegan
 * antes del límite (permiso denegado, datos inválidos) se propagan como excepción.
 */
internal suspend fun awaitWriteOrQueued(
    timeout: Duration = PROFILE_WRITE_CONFIRMATION_TIMEOUT,
    write: suspend () -> Unit,
) {
    withTimeoutOrNull(timeout) { write() }
}

/**
 * Ejecuta [block] ignorando únicamente los fallos de red.
 *
 * Sirve para pasos opcionales que Firebase no encola sin conexión (por ejemplo el nombre visible de
 * Auth, que ya queda guardado en el documento de perfil). Cualquier otro error, incluida la
 * cancelación, se propaga.
 */
internal suspend fun ignoringNetworkFailure(block: suspend () -> Unit) {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        if (error.toProfileOperationError() !is NetworkUnavailableException) throw error
    }
}

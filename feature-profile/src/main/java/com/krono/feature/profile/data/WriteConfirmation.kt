package com.krono.feature.profile.data

import com.krono.feature.profile.domain.NetworkUnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Espera máxima a la confirmación del servidor de una escritura de perfil, preferencias o ajustes.
 *
 * Con red, Firestore suele confirmar en menos de un segundo; sin red, el usuario solo espera este tiempo.
 */
internal val PROFILE_WRITE_CONFIRMATION_TIMEOUT: Duration = 2.seconds

/**
 * Resultado explícito de la confirmación de escritura en Firestore para el módulo de perfil.
 */
sealed interface ProfileWriteConfirmation {
    /** Confirmada por el servidor en la nube dentro del límite de tiempo. */
    data object Confirmed : ProfileWriteConfirmation

    /** Tiempo límite agotado; la escritura permanece encolada en la caché local para sincronización posterior. */
    data object PendingSync : ProfileWriteConfirmation
}

/**
 * Ejecuta [write] (que suspende hasta la confirmación del servidor) esperando como mucho [timeout].
 *
 * Firestore aplica cada escritura al instante en su caché local y la encola para el servidor, por lo
 * que si vence el límite la escritura se da por encolada ([ProfileWriteConfirmation.PendingSync]).
 * Los errores reales (permiso denegado, datos inválidos) se propagan como excepción.
 *
 * @param timeout Tiempo límite de espera a la respuesta del servidor remoto.
 * @param write Lambda de escritura suspendida.
 * @return [ProfileWriteConfirmation.Confirmed] si se recibió confirmación, o [ProfileWriteConfirmation.PendingSync] si venció el tiempo.
 */
internal suspend fun awaitWriteOrQueued(
    timeout: Duration = PROFILE_WRITE_CONFIRMATION_TIMEOUT,
    write: suspend () -> Unit,
): ProfileWriteConfirmation {
    val completed = withTimeoutOrNull(timeout) {
        write()
        true
    }
    return if (completed == true) {
        ProfileWriteConfirmation.Confirmed
    } else {
        ProfileWriteConfirmation.PendingSync
    }
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

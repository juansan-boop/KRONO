package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.ui.ProfileLog
import kotlinx.coroutines.CancellationException

/**
 * Ejecuta una sincronización remota cuyo fallo no debe deshacer el cambio local ya guardado.
 *
 * El cambio local es la fuente inmediata de la UI; si el servidor lo rechaza o no hay red, el fallo
 * solo se registra en logcat (sin datos del usuario) y la próxima sincronización desde Firestore
 * converge al valor del servidor. La cancelación sí se propaga.
 *
 * @param operation Nombre de la operación, solo para el registro.
 * @param call Llamada al origen de datos remoto.
 */
internal suspend fun syncRemoteBestEffort(operation: String, call: suspend () -> KronoResult<Unit>) {
    try {
        val result = call()
        if (result is KronoResult.Error) ProfileLog.failure(operation, result.throwable)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        ProfileLog.failure(operation, error)
    }
}

package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.ui.ProfileLog
import kotlinx.coroutines.CancellationException

/**
 * Ejecuta una sincronización remota registrando el resultado y retornándolo explícitamente
 * para evitar el éxito silencioso ante fallos remotos.
 *
 * @param operation Nombre de la operación para el registro y trazabilidad.
 * @param call Llamada suspendida al origen de datos remoto.
 * @return [KronoResult] con el resultado de la llamada remota.
 */
internal suspend fun syncRemoteBestEffort(
    operation: String,
    call: suspend () -> KronoResult<Unit>,
): KronoResult<Unit> {
    return try {
        val result = call()
        if (result is KronoResult.Error) {
            ProfileLog.failure(operation, result.throwable)
        }
        result
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        ProfileLog.failure(operation, error)
        KronoResult.Error(error)
    }
}

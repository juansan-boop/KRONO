package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Contrato de lectura de las sesiones de tiempo registradas (Sección 6 del SPEC). Las escrituras
 * van por [TimerRepository], que las crea al finalizar el cronómetro.
 */
interface TimeSessionRepository {

    /**
     * Observa las sesiones iniciadas en `[from, to)` del usuario autenticado, en tiempo real.
     * Sin conexión se siguen emitiendo las de la caché local.
     *
     * @param from Inicio inclusivo del intervalo.
     * @param to Fin exclusivo del intervalo.
     * @return [Flow] con la lista ante cada cambio, o un único [KronoResult.Error] (y termina) si
     * la observación falla.
     */
    fun observeSessionsBetween(from: Instant, to: Instant): Flow<KronoResult<List<TimeSession>>>
}

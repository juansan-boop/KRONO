package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de persistencia del cronómetro y de las sesiones de tiempo (F-08).
 *
 * Las escrituras no esperan indefinidamente al servidor: si la confirmación no llega a tiempo
 * (sin conexión), la escritura ya quedó encolada en la caché local y se informa como éxito.
 * Los errores que sí llegan (permiso denegado, datos inválidos) se devuelven como
 * [KronoResult.Error].
 */
interface TimerRepository {

    /** Observa el cronómetro en curso del usuario, o `null` si no hay ninguno. */
    fun observeActiveTimer(): Flow<ActiveTimer?>

    /** Lee una vez el cronómetro en curso, o `null` si no hay ninguno. */
    suspend fun getActiveTimer(): KronoResult<ActiveTimer?>

    /**
     * Guarda el cronómetro en curso, reemplazando el anterior (solo hay uno, CA-4). Reemplazarlo
     * también descarta un cronómetro huérfano cuya tarea ya no existe.
     */
    suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit>

    /**
     * Cierra el cronómetro en curso de forma atómica (CA-2): registra [session], suma su
     * duración al `actualDuration` de la tarea y elimina el cronómetro en curso.
     *
     * Guardar dos veces una sesión con el mismo id la reemplaza, sin duplicarla. Si la tarea ya no
     * existe (se borró mientras corría el cronómetro), la sesión se conserva igualmente, sin
     * sumarse a nada, y el resultado lo indica con [FinishedSession.addedToTask] en `false`.
     */
    suspend fun finishActiveTimer(session: TimeSession): KronoResult<FinishedSession>
}

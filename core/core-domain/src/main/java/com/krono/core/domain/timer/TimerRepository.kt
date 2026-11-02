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

    /**
     * Lee una vez el cronómetro en curso, o `null` si no hay ninguno. No espera al servidor
     * indefinidamente: sin conexión usa la caché local, y si el cronómetro tampoco está ahí
     * responde `null`, para que iniciar sin red no falle.
     */
    suspend fun getActiveTimer(): KronoResult<ActiveTimer?>

    /**
     * Guarda el cronómetro en curso, reemplazando el anterior (solo hay uno, CA-4). Reemplazarlo
     * también descarta un cronómetro huérfano cuya tarea ya no existe.
     */
    suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit>

    /**
     * Cierra el cronómetro en curso (CA-2): registra [session], elimina el cronómetro en curso y
     * suma la duración al `actualDuration` de la tarea.
     *
     * Es idempotente: si el cronómetro de [session] ya se finalizó (por ejemplo, un doble toque),
     * devuelve el mismo resultado sin volver a sumar; si el cronómetro en curso es otro, devuelve
     * [TimerError.NoActiveTimer] sin tocarlo. Si la tarea ya no existe (se borró mientras corría
     * el cronómetro), la sesión se conserva igualmente, sin sumarse a nada, y el resultado lo
     * indica con [FinishedSession.addedToTask] en `false`.
     */
    suspend fun finishActiveTimer(session: TimeSession): KronoResult<FinishedSession>

    /**
     * Descarta el cronómetro en curso sin registrar ninguna sesión ni sumar tiempo a la tarea
     * (sesiones demasiado cortas, [FinishTimerUseCase.MIN_SESSION_DURATION]).
     *
     * Es idempotente: solo elimina el cronómetro cuya [TimeSession] tendría el id [sessionId];
     * si ya no hay uno, o es otro, no hace nada y devuelve éxito.
     */
    suspend fun discardActiveTimer(sessionId: String): KronoResult<Unit>
}

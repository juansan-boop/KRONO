package com.krono.core.domain.timer

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.Flow

/** Contrato de persistencia del cronómetro y de las sesiones de tiempo (F-08). */
interface TimerRepository {

    /** Observa el cronómetro en curso del usuario, o `null` si no hay ninguno. */
    fun observeActiveTimer(): Flow<ActiveTimer?>

    /** Lee una vez el cronómetro en curso, o `null` si no hay ninguno. */
    suspend fun getActiveTimer(): KronoResult<ActiveTimer?>

    /** Guarda el cronómetro en curso, reemplazando el anterior (solo hay uno, CA-4). */
    suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit>

    /**
     * Cierra el cronómetro en curso de forma atómica (CA-2): registra [session], suma su
     * duración al `actualDuration` de la tarea y elimina el cronómetro en curso.
     */
    suspend fun finishActiveTimer(session: TimeSession): KronoResult<Unit>
}

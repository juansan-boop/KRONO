package com.krono.core.domain.calendar

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Observa la agenda de un [CalendarPeriod] a partir de las tareas del usuario (F-16 CA-1 y CA-2).
 *
 * Reutiliza [TaskRepository.observeTasks], así que se actualiza ante cualquier cambio de las
 * tareas. Un error del repositorio se propaga como [KronoResult.Error]; nunca se convierte en
 * una agenda vacía.
 */
class ObserveCalendarUseCase(
    private val repository: TaskRepository,
) {
    /**
     * @param period Período a consultar (día, semana o mes).
     * @return [Flow] con la [CalendarSchedule] del período o el error de la observación.
     */
    operator fun invoke(period: CalendarPeriod): Flow<KronoResult<CalendarSchedule>> =
        repository.observeTasks().map { result ->
            when (result) {
                is KronoResult.Success -> KronoResult.Success(CalendarSchedule.from(result.data, period))
                is KronoResult.Error -> result
                KronoResult.Loading -> KronoResult.Loading
            }
        }
}

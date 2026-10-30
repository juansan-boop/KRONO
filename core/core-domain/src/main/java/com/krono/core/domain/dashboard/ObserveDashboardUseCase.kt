package com.krono.core.domain.dashboard

import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskRepository
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimeSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Observa el [DashboardSummary] de un día a partir de las tareas y de las sesiones de tiempo
 * (F-13 CA-1 y CA-3), todo vía `core-domain` (CA-4).
 *
 * Se actualiza ante cualquier cambio de las tareas o de las sesiones. Si cualquiera de las dos
 * lecturas falla emite [KronoResult.Error] en lugar de un resumen en ceros, que sería falso.
 */
class ObserveDashboardUseCase(
    private val taskRepository: TaskRepository,
    private val sessionRepository: TimeSessionRepository,
    private val dateTimeProvider: CurrentDateTimeProvider,
) {
    /**
     * @param day Día a resumir; los límites del día salen de la zona horaria del usuario.
     * @return [Flow] con el resumen del día o el error de la observación.
     */
    operator fun invoke(day: LocalDate): Flow<KronoResult<DashboardSummary>> {
        val from = dateTimeProvider.startOfDay(day)
        val to = dateTimeProvider.startOfDay(day.plus(DatePeriod(days = 1)))
        return combine(
            taskRepository.observeTasks(),
            sessionRepository.observeSessionsBetween(from, to),
        ) { tasks, sessions -> summarize(tasks, sessions, day) }
    }

    private fun summarize(
        tasks: KronoResult<List<Task>>,
        sessions: KronoResult<List<TimeSession>>,
        day: LocalDate,
    ): KronoResult<DashboardSummary> = when {
        tasks is KronoResult.Error -> tasks
        sessions is KronoResult.Error -> sessions
        tasks is KronoResult.Success && sessions is KronoResult.Success ->
            KronoResult.Success(DashboardSummary.from(tasks.data, sessions.data, day))
        else -> KronoResult.Loading
    }
}

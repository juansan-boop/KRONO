package com.krono.feature.calendar

import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CalendarBlock
import com.krono.core.domain.calendar.CalendarPeriod
import com.krono.core.domain.calendar.CalendarSchedule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Traduce la agenda del dominio a [CalendarUiState] para un mes y un día seleccionado.
 *
 * Toda la lógica de fechas (rejilla, días vecinos, bloques por día, "En curso") sale de
 * `core-domain`; aquí solo se arma el modelo de la pantalla.
 *
 * @param period Mes mostrado.
 * @param selectedDate Día cuya agenda se muestra.
 * @param today Fecha actual, resaltada en la rejilla.
 * @param now Hora actual, para marcar las tareas "En curso".
 */
internal class CalendarStateBuilder(
    private val period: CalendarPeriod,
    private val selectedDate: LocalDate,
    private val today: LocalDate,
    private val now: LocalDateTime,
) {
    /** Estado de la pantalla para el [result] de observar el mes. */
    fun build(result: KronoResult<CalendarSchedule>): CalendarUiState {
        val schedule = (result as? KronoResult.Success)?.data
        return CalendarUiState(
            displayedMonth = period.anchor,
            today = today,
            selectedDate = selectedDate,
            weeks = period.weeks.map { week -> week.map { dayCell(it, schedule) } },
            agenda = agendaState(result),
        )
    }

    private fun dayCell(date: LocalDate, schedule: CalendarSchedule?): CalendarDayUi {
        val blocks = schedule?.blocksOn(date)
        return CalendarDayUi(
            date = date,
            isInDisplayedMonth = period.isInAnchorMonth(date),
            isToday = date == today,
            isSelected = date == selectedDate,
            taskCount = blocks?.size,
            categories = blocks.orEmpty().map { it.category }.distinct().take(MAX_DAY_INDICATORS),
        )
    }

    private fun agendaState(result: KronoResult<CalendarSchedule>): DayAgendaState = when (result) {
        KronoResult.Loading -> DayAgendaState.Loading
        is KronoResult.Error -> DayAgendaState.LoadError
        is KronoResult.Success -> DayAgendaState.Loaded(result.data.blocksOn(selectedDate).map { it.toAgendaItem() })
    }

    private fun CalendarBlock.toAgendaItem() = AgendaItemUi(
        taskId = taskId,
        title = task.title,
        note = task.note?.takeIf { it.isNotBlank() },
        start = start,
        end = end,
        category = category,
        priority = priority,
        status = status(),
    )

    private fun CalendarBlock.status(): AgendaItemStatus = when {
        isCompleted -> AgendaItemStatus.COMPLETED
        isInProgress(now) -> AgendaItemStatus.IN_PROGRESS
        else -> AgendaItemStatus.SCHEDULED
    }
}

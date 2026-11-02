package com.krono.feature.calendar

import com.krono.core.common.KronoResult
import com.krono.core.domain.calendar.CalendarBlock
import com.krono.core.domain.calendar.CalendarPeriod
import com.krono.core.domain.calendar.CalendarSchedule
import com.krono.core.domain.calendar.CalendarViewMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import java.time.temporal.IsoFields

/**
 * Traduce la agenda del dominio a [CalendarUiState] para una vista, un período y un día
 * seleccionado.
 *
 * Toda la lógica de fechas (rejilla, días vecinos, bloques por día, "En curso") sale de
 * `core-domain`; aquí solo se arma el modelo de la pantalla.
 *
 * @param mode Vista activa.
 * @param period Período cuya agenda se cargó: el mes en Mes y la semana en Semana y Día (la vista
 * diaria muestra la franja de su semana, así que comparte la carga con Semana).
 * @param selectedDate Día cuya agenda se muestra.
 * @param today Fecha actual, resaltada en la rejilla o la franja.
 * @param now Hora actual, para marcar las tareas "En curso" y colocar el separador "Ahora".
 */
internal class CalendarStateBuilder(
    private val mode: CalendarViewMode,
    private val period: CalendarPeriod,
    private val selectedDate: LocalDate,
    private val today: LocalDate,
    private val now: LocalDateTime,
) {
    /** Estado de la pantalla para el [result] de observar el período. */
    fun build(result: KronoResult<CalendarSchedule>): CalendarUiState {
        val schedule = (result as? KronoResult.Success)?.data
        return CalendarUiState(
            mode = mode,
            today = today,
            selectedDate = selectedDate,
            weekNumber = selectedDate.toJavaLocalDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
            weeks = period.weeks.map { week -> week.map { dayCell(it, schedule) } },
            agenda = agendaState(result),
        )
    }

    private fun dayCell(date: LocalDate, schedule: CalendarSchedule?): CalendarDayUi {
        val blocks = schedule?.blocksOn(date)
        return CalendarDayUi(
            date = date,
            // Solo la rejilla mensual atenúa días: la franja semanal pertenece entera al período.
            isInDisplayedPeriod = mode != CalendarViewMode.MONTH || period.isInAnchorMonth(date),
            isToday = date == today,
            isSelected = date == selectedDate,
            taskCount = blocks?.size,
            categories = blocks.orEmpty().map { it.category }.distinct().take(MAX_DAY_INDICATORS),
        )
    }

    private fun agendaState(result: KronoResult<CalendarSchedule>): DayAgendaState = when (result) {
        KronoResult.Loading -> DayAgendaState.Loading
        is KronoResult.Error -> DayAgendaState.LoadError
        is KronoResult.Success -> {
            val items = result.data.blocksOn(selectedDate).map { it.toAgendaItem() }
            DayAgendaState.Loaded(items, nowMarkerFor(items))
        }
    }

    /**
     * El separador "Ahora" solo existe en la vista diaria de hoy y con tareas que separar. Sigue el
     * orden cronológico de la agenda: va después de toda tarea que ya empezó (terminada o en
     * curso) y antes de la primera que empieza más tarde; si todas ya empezaron, al final.
     */
    private fun nowMarkerFor(items: List<AgendaItemUi>): NowMarker? {
        if (mode != CalendarViewMode.DAY || selectedDate != today || items.isEmpty()) return null
        val firstUpcoming = items.indexOfFirst { it.start > now }
        return NowMarker(beforeIndex = if (firstUpcoming >= 0) firstUpcoming else items.size, time = now)
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

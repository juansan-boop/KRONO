package com.krono.core.domain.calendar

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Primer día de la semana del calendario. */
val WEEK_START: DayOfWeek = DayOfWeek.MONDAY

private const val DAYS_PER_WEEK = 7

/** Vistas del calendario (F-16 CA-1). */
enum class CalendarViewMode {
    /** Un solo día. */
    DAY,

    /** Una semana de lunes a domingo. */
    WEEK,

    /** Un mes, mostrado como rejilla de semanas completas. */
    MONTH,
}

/**
 * Período visible del calendario: una vista ([mode]) alrededor de una fecha ([anchor]).
 *
 * Todos los límites son de fecha: [firstDay] inclusivo y [endExclusive] exclusivo. En la vista
 * mensual el período es la rejilla de semanas completas (lunes a domingo), que incluye días de
 * los meses vecinos; [isInAnchorMonth] distingue los del mes propio.
 *
 * @property mode Vista activa.
 * @property anchor Cualquier fecha del período. Tras navegar en la vista mensual es el día 1.
 */
data class CalendarPeriod(
    val mode: CalendarViewMode,
    val anchor: LocalDate,
) {
    /** Primer día visible (inclusivo). */
    val firstDay: LocalDate = when (mode) {
        CalendarViewMode.DAY -> anchor
        CalendarViewMode.WEEK -> startOfWeek(anchor)
        CalendarViewMode.MONTH -> startOfWeek(LocalDate(anchor.year, anchor.month, 1))
    }

    /** Día siguiente al último visible (exclusivo). */
    val endExclusive: LocalDate = when (mode) {
        CalendarViewMode.DAY -> firstDay.plus(1, DateTimeUnit.DAY)
        CalendarViewMode.WEEK -> firstDay.plus(1, DateTimeUnit.WEEK)
        CalendarViewMode.MONTH -> startOfWeek(lastDayOfMonth()).plus(1, DateTimeUnit.WEEK)
    }

    /** Semanas visibles: una lista de 7 días por semana (o un solo día en la vista diaria). */
    val weeks: List<List<LocalDate>> by lazy {
        if (mode == CalendarViewMode.DAY) listOf(listOf(anchor)) else daysBetween().chunked(DAYS_PER_WEEK)
    }

    /** Todos los días visibles, en orden. */
    val days: List<LocalDate> get() = weeks.flatten()

    /** Indica si [date] está dentro del período: [firstDay] inclusivo, [endExclusive] exclusivo. */
    fun contains(date: LocalDate): Boolean = date >= firstDay && date < endExclusive

    /** Indica si [date] pertenece al mes de [anchor] (para atenuar los días vecinos de la rejilla). */
    fun isInAnchorMonth(date: LocalDate): Boolean = date.year == anchor.year && date.month == anchor.month

    /** Período siguiente: un día, una semana o un mes (al día 1 del mes siguiente). */
    fun next(): CalendarPeriod = shifted(1)

    /** Período anterior: un día, una semana o un mes (al día 1 del mes anterior). */
    fun previous(): CalendarPeriod = shifted(-1)

    private fun shifted(steps: Int): CalendarPeriod = when (mode) {
        CalendarViewMode.DAY -> copy(anchor = anchor.plus(steps, DateTimeUnit.DAY))
        CalendarViewMode.WEEK -> copy(anchor = anchor.plus(steps, DateTimeUnit.WEEK))
        // Se normaliza al día 1 para que avanzar y retroceder sean inversos (31 de enero -> febrero).
        CalendarViewMode.MONTH -> copy(anchor = LocalDate(anchor.year, anchor.month, 1).plus(steps, DateTimeUnit.MONTH))
    }

    private fun lastDayOfMonth(): LocalDate =
        LocalDate(anchor.year, anchor.month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

    private fun daysBetween(): List<LocalDate> =
        generateSequence(firstDay) { it.plus(1, DateTimeUnit.DAY) }.takeWhile { it < endExclusive }.toList()
}

private fun startOfWeek(date: LocalDate): LocalDate {
    val daysSinceWeekStart = (date.dayOfWeek.isoDayNumber - WEEK_START.isoDayNumber + DAYS_PER_WEEK) % DAYS_PER_WEEK
    return date.minus(daysSinceWeekStart, DateTimeUnit.DAY)
}

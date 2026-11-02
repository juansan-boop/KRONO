package com.krono.feature.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.feature.calendar.CalendarDayUi
import com.krono.feature.calendar.CalendarUiState
import com.krono.feature.calendar.R
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Idioma de los nombres de meses y días: todo el copy de KRONO está en español. */
internal val CalendarLocale: Locale = Locale.forLanguageTag("es")

private val FullDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(CalendarLocale)

/** Pone en mayúscula la primera letra ("octubre" a "Octubre"), como en los títulos del diseño. */
private fun String.capitalized(): String = replaceFirstChar { it.titlecase(CalendarLocale) }

/** Título del mes mostrado, por ejemplo "Octubre 2024". */
@Composable
internal fun monthTitle(month: LocalDate): String = stringResource(
    R.string.calendar_month_title,
    month.month.getDisplayName(TextStyle.FULL_STANDALONE, CalendarLocale).capitalized(),
    month.year,
)

/** Inicial del día de la semana para la cabecera de la rejilla ("L", "M", "X"...). */
internal fun DayOfWeek.narrowName(): String = getDisplayName(TextStyle.NARROW, CalendarLocale).uppercase(CalendarLocale)

/** Nombre completo del día de la semana, para lectores de pantalla ("miércoles"). */
internal fun DayOfWeek.fullName(): String = getDisplayName(TextStyle.FULL_STANDALONE, CalendarLocale)

/** Fecha completa para lectores de pantalla, por ejemplo "jueves, 24 de octubre de 2024". */
internal fun LocalDate.spokenDate(): String = FullDateFormatter.format(toJavaLocalDate())

/** Etiqueta corta del día seleccionado en la agenda, por ejemplo "Jueves 24". */
@Composable
internal fun agendaDayLabel(date: LocalDate): String =
    stringResource(R.string.calendar_agenda_day_chip, date.dayOfWeek.fullName().capitalized(), date.dayOfMonth)

/**
 * Descripción hablada de una celda: la fecha y, si se conocen, sus tareas
 * ("jueves, 24 de octubre de 2024, 3 tareas").
 */
@Composable
internal fun dayCellDescription(day: CalendarDayUi): String {
    val count = day.taskCount ?: return day.date.spokenDate()
    val tasks = if (count == 0) {
        stringResource(R.string.calendar_day_no_tasks)
    } else {
        pluralStringResource(R.plurals.calendar_day_tasks, count, count)
    }
    return stringResource(R.string.calendar_day_description, day.date.spokenDate(), tasks)
}

/** Hora en formato de 24 horas con dos dígitos ("09:00"). */
@Composable
internal fun clockTime(dateTime: LocalDateTime): String =
    stringResource(R.string.calendar_time, dateTime.hour, dateTime.minute)

/**
 * Textos del encabezado de la vista activa.
 *
 * @property eyebrow Línea pequeña en mayúsculas sobre el título ("SEMANA 43"), o `null` en la vista mensual.
 * @property title Título principal ("Octubre 2024", "21 - 27 de octubre", "26 oct - 1 nov", "24 de octubre").
 * @property description Texto completo para los lectores de pantalla cuando [title] va abreviado
 * ("26 de octubre - 1 de noviembre"), o `null` si [title] ya se lee bien.
 */
internal data class CalendarTitle(val eyebrow: String?, val title: String, val description: String? = null)

/** Título del encabezado según la vista: mes y año, rango de la semana o fecha del día. */
@Composable
internal fun calendarTitle(state: CalendarUiState): CalendarTitle = when (state.mode) {
    CalendarViewMode.MONTH -> CalendarTitle(eyebrow = null, title = monthTitle(state.selectedDate))
    CalendarViewMode.WEEK -> CalendarTitle(
        eyebrow = stringResource(R.string.calendar_week_eyebrow, state.weekNumber).uppercase(CalendarLocale),
        title = weekRangeTitle(state.weeks.single().map { it.date }),
        description = weekRangeDescription(state.weeks.single().map { it.date }),
    )
    CalendarViewMode.DAY -> CalendarTitle(
        eyebrow = stringResource(
            R.string.calendar_day_eyebrow,
            state.selectedDate.dayOfWeek.fullName(),
            state.weekNumber,
        ).uppercase(CalendarLocale),
        title = stringResource(R.string.calendar_day_title, state.selectedDate.dayOfMonth, state.selectedDate.monthName()),
    )
}

/**
 * Rango de una semana de lunes a domingo, por ejemplo "21 - 27 de octubre". Si cruza de mes se
 * abrevia ("26 oct - 1 nov") para que quepa en una sola línea y el encabezado mida lo mismo que
 * el de las otras vistas.
 */
@Composable
private fun weekRangeTitle(days: List<LocalDate>): String {
    val first = days.first()
    val last = days.last()
    return if (first.month == last.month) {
        stringResource(R.string.calendar_week_range_same_month, first.dayOfMonth, last.dayOfMonth, last.monthName())
    } else {
        stringResource(
            R.string.calendar_week_range_two_months,
            first.dayOfMonth,
            first.shortMonthName(),
            last.dayOfMonth,
            last.shortMonthName(),
        )
    }
}

/** Lectura completa del rango cuando el título va abreviado; `null` si no se abrevió. */
@Composable
private fun weekRangeDescription(days: List<LocalDate>): String? {
    val first = days.first()
    val last = days.last()
    if (first.month == last.month) return null
    return stringResource(
        R.string.calendar_week_range_two_months_description,
        first.dayOfMonth,
        first.monthName(),
        last.dayOfMonth,
        last.monthName(),
    )
}

private fun LocalDate.shortMonthName(): String = month.getDisplayName(TextStyle.SHORT_STANDALONE, CalendarLocale)

private fun LocalDate.monthName(): String = month.getDisplayName(TextStyle.FULL_STANDALONE, CalendarLocale)

/** Descripción del botón "anterior" según la vista ("Semana anterior"). */
@Composable
internal fun previousPeriodDescription(mode: CalendarViewMode): String = stringResource(
    when (mode) {
        CalendarViewMode.MONTH -> R.string.calendar_previous_month
        CalendarViewMode.WEEK -> R.string.calendar_previous_week
        CalendarViewMode.DAY -> R.string.calendar_previous_day
    },
)

/** Descripción del botón "siguiente" según la vista ("Semana siguiente"). */
@Composable
internal fun nextPeriodDescription(mode: CalendarViewMode): String = stringResource(
    when (mode) {
        CalendarViewMode.MONTH -> R.string.calendar_next_month
        CalendarViewMode.WEEK -> R.string.calendar_next_week
        CalendarViewMode.DAY -> R.string.calendar_next_day
    },
)

/** Abreviatura de tres letras del día de la semana para la franja ("LUN", "MIÉ"). */
internal fun DayOfWeek.shortName(): String =
    getDisplayName(TextStyle.SHORT, CalendarLocale).removeSuffix(".").uppercase(CalendarLocale)

/** Texto del separador "Ahora · 14:15" de la vista diaria. */
@Composable
internal fun nowMarkerLabel(time: LocalDateTime): String =
    stringResource(R.string.calendar_now_marker, clockTime(time))

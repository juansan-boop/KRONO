package com.krono.feature.calendar.month.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.krono.feature.calendar.R
import com.krono.feature.calendar.month.CalendarDayUi
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

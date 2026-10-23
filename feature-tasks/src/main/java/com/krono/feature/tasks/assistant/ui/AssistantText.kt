package com.krono.feature.tasks.assistant.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.PriorityFactor
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.feature.tasks.R
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

// Textos del asistente de prioridades (F-12) a partir de los datos del plan.

private const val MINUTES_PER_HOUR = 60L

/** Duración legible: "45 min", "2 h" o "1 h 30 m". */
@Composable
internal fun durationText(duration: Duration): String {
    val totalMinutes = duration.absoluteValue.inWholeMinutes
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return when {
        hours == 0L -> stringResource(R.string.duration_minutes, minutes.toInt())
        minutes == 0L -> stringResource(R.string.duration_hours, hours.toInt())
        else -> stringResource(R.string.duration_hours_minutes, hours.toInt(), minutes.toInt())
    }
}

/** Hora del día en formato de 24 horas, por ejemplo "14:15". */
@Composable
internal fun clockText(dateTime: LocalDateTime): String =
    stringResource(R.string.assistant_clock_time, dateTime.hour, dateTime.minute)

/** Rango horario del bloque sugerido, por ejemplo "14:15 - 15:00". */
@Composable
internal fun timeRangeText(planned: PlannedTask): String =
    stringResource(R.string.assistant_time_range, clockText(planned.start), clockText(planned.end))

/** "Vence en 4 h", "Venció hace 30 min" o "Sin fecha de entrega". */
@Composable
internal fun dueText(planned: PlannedTask): String {
    val timeUntilDue = planned.timeUntilDue ?: return stringResource(R.string.assistant_no_due_date)
    val amount = durationText(timeUntilDue)
    return if (timeUntilDue.isNegative()) {
        stringResource(R.string.assistant_overdue_by, amount)
    } else {
        stringResource(R.string.assistant_due_in, amount)
    }
}

/** Duración del bloque con su origen, por ejemplo "45 min · Según tu historial". */
@Composable
internal fun estimateText(planned: PlannedTask): String =
    stringResource(
        R.string.assistant_duration_with_source,
        durationText(planned.estimate),
        stringResource(planned.estimateSource.labelRes()),
    )

/**
 * Explicación de respaldo cuando la IA no responde: una frase por motivo, en el orden de
 * [PriorityFactor], para que siempre haya un "por qué" verificable.
 */
@Composable
internal fun templateExplanation(factors: Set<PriorityFactor>): String {
    if (factors.isEmpty()) return stringResource(R.string.assistant_reason_default)
    return PriorityFactor.entries
        .filter { it in factors }
        .map { stringResource(it.reasonRes()) }
        .joinToString(separator = " ")
}

/** Nombre visible de la categoría. */
@StringRes
internal fun TaskCategory.labelRes(): Int = when (this) {
    TaskCategory.ESTUDIO -> R.string.category_study
    TaskCategory.TRABAJO -> R.string.category_work
    TaskCategory.PERSONAL -> R.string.category_personal
}

/** Etiqueta "Prioridad alta/media/baja". */
@StringRes
internal fun TaskPriority.assistantLabelRes(): Int = when (this) {
    TaskPriority.ALTA -> R.string.assistant_priority_high
    TaskPriority.MEDIA -> R.string.assistant_priority_medium
    TaskPriority.BAJA -> R.string.assistant_priority_low
}

@StringRes
private fun EstimateSource.labelRes(): Int = when (this) {
    EstimateSource.MANUAL -> R.string.assistant_estimate_manual
    EstimateSource.HISTORY -> R.string.assistant_estimate_history
    EstimateSource.DEFAULT -> R.string.assistant_estimate_default
}

@StringRes
private fun PriorityFactor.reasonRes(): Int = when (this) {
    PriorityFactor.OVERDUE -> R.string.assistant_reason_overdue
    PriorityFactor.DUE_VERY_SOON -> R.string.assistant_reason_due_very_soon
    PriorityFactor.DUE_TODAY -> R.string.assistant_reason_due_today
    PriorityFactor.DUE_SOON -> R.string.assistant_reason_due_soon
    PriorityFactor.HIGH_PRIORITY -> R.string.assistant_reason_high_priority
    PriorityFactor.TIGHT_SCHEDULE -> R.string.assistant_reason_tight_schedule
}

package com.krono.feature.dashboard.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.Tertiary
import com.krono.feature.dashboard.R
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

private const val MINUTES_PER_HOUR = 60

/** Tiempo de enfoque legible: "45 min", "2 h" o "1 h 30 min". */
@Composable
internal fun focusTimeText(duration: Duration): String {
    val totalMinutes = duration.inWholeMinutes.toInt()
    val hours = totalMinutes / MINUTES_PER_HOUR
    val minutes = totalMinutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.dashboard_focus_minutes, minutes)
        minutes == 0 -> stringResource(R.string.dashboard_focus_hours, hours)
        else -> stringResource(R.string.dashboard_focus_hours_minutes, hours, minutes)
    }
}

/** Hora de 24 horas ("09:30"), igual que el calendario. */
@Composable
internal fun clockTimeText(time: LocalTime): String =
    stringResource(R.string.dashboard_time, time.hour, time.minute)

/** Color de acento de la categoría, de la paleta del diseño (mismo criterio que el calendario). */
internal val TaskCategory.accentColor: Color
    get() = when (this) {
        TaskCategory.ESTUDIO -> PrimaryFixedDim
        TaskCategory.TRABAJO -> SecondaryCyan
        TaskCategory.PERSONAL -> Tertiary
    }

/** Nombre visible de la categoría. */
@get:StringRes
internal val TaskCategory.labelRes: Int
    get() = when (this) {
        TaskCategory.ESTUDIO -> R.string.dashboard_category_study
        TaskCategory.TRABAJO -> R.string.dashboard_category_work
        TaskCategory.PERSONAL -> R.string.dashboard_category_personal
    }

/** Texto visible de la prioridad ("Prioridad alta"). */
@get:StringRes
internal val TaskPriority.labelRes: Int
    get() = when (this) {
        TaskPriority.ALTA -> R.string.dashboard_priority_high
        TaskPriority.MEDIA -> R.string.dashboard_priority_medium
        TaskPriority.BAJA -> R.string.dashboard_priority_low
    }

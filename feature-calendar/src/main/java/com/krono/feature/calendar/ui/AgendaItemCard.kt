package com.krono.feature.calendar.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.AgendaItemStatus
import com.krono.feature.calendar.AgendaItemUi
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Tarjeta de una tarea en la agenda del día (F-16 CA-2): horario de inicio a fin con el punto de
 * su categoría, estado ("En curso" o "Completada"), título, nota, etiquetas de categoría y
 * prioridad, y acción rápida para reprogramar la tarea. Tocar la tarjeta abre el detalle de la tarea.
 *
 * @param item Tarea a mostrar.
 * @param onClick Abre el detalle de la tarea.
 * @param onReschedule Reprograma la tarea a una nueva fecha y hora.
 * @param modifier Modificador de diseño.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AgendaItemCard(
    item: AgendaItemUi,
    onClick: () -> Unit,
    onReschedule: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isInProgress = item.status == AgendaItemStatus.IN_PROGRESS
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(
                borderColor = if (isInProgress) PrimaryFixedDim.copy(alpha = IN_PROGRESS_BORDER_ALPHA) else KronoGlass.borderColor,
            )
            .clickable(onClickLabel = stringResource(R.string.calendar_open_task), onClick = onClick)
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlowRow(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                TimeRange(item)
                StatusBadge(item.status)
            }
            RescheduleButton(
                item = item,
                onReschedule = onReschedule,
            )
        }
        Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = OnSurface)
        item.note?.let { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                maxLines = NOTE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            TaskLabel(text = stringResource(item.category.labelRes), color = item.category.accentColor)
            TaskLabel(text = stringResource(item.priority.labelRes), color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun RescheduleButton(
    item: AgendaItemUi,
    onReschedule: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rescheduleLabel = stringResource(R.string.calendar_reschedule_task)

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .size(KronoSizes.minTouchTarget)
                .semantics {
                    contentDescription = rescheduleLabel
                },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                modifier = Modifier.size(KronoSizes.iconMedium),
                tint = OnSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceContainerHigh),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.calendar_reschedule_plus_1h),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                },
                onClick = {
                    expanded = false
                    val newTime = item.end.addHours(1)
                    onReschedule(newTime)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.calendar_reschedule_tomorrow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                },
                onClick = {
                    expanded = false
                    val newTime = item.end.addDays(1)
                    onReschedule(newTime)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.calendar_reschedule_next_week),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                    )
                },
                onClick = {
                    expanded = false
                    val newTime = item.end.addDays(7)
                    onReschedule(newTime)
                },
            )
        }
    }
}

/** Añade [hours] horas a [LocalDateTime] respetando la zona horaria del sistema. */
private fun LocalDateTime.addHours(hours: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    val instant = this.toInstant(timeZone)
    return (instant + hours.hours).toLocalDateTime(timeZone)
}

/** Añade [days] días a [LocalDateTime] respetando la zona horaria del sistema. */
private fun LocalDateTime.addDays(days: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    val instant = this.toInstant(timeZone)
    return (instant + days.days).toLocalDateTime(timeZone)
}

@Composable
private fun TimeRange(item: AgendaItemUi) {
    val start = clockTime(item.start)
    val end = clockTime(item.end)
    Row(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KronoSizes.calendarCategoryDot)
                .background(item.category.accentColor, CircleShape),
        )
        Text(
            text = stringResource(R.string.calendar_time_range, start, end),
            style = MaterialTheme.typography.labelLarge,
            color = item.category.accentColor,
        )
    }
}

@Composable
private fun StatusBadge(status: AgendaItemStatus) {
    when (status) {
        AgendaItemStatus.IN_PROGRESS ->
            TaskLabel(text = stringResource(R.string.calendar_status_in_progress), color = PrimaryFixedDim)
        AgendaItemStatus.COMPLETED ->
            TaskLabel(text = stringResource(R.string.calendar_status_completed), color = SecondaryCyan, icon = KronoIcons.CheckCircle)
        AgendaItemStatus.SCHEDULED -> Unit
    }
}

@Composable
private fun TaskLabel(text: String, color: Color, @DrawableRes icon: Int? = null) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = LABEL_BACKGROUND_ALPHA), KronoShapes.extraSmall)
            .border(KronoGlass.borderWidth, color.copy(alpha = LABEL_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(painter = painterResource(it), contentDescription = null, modifier = Modifier.size(KronoSizes.iconSmall), tint = color)
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

private const val IN_PROGRESS_BORDER_ALPHA = 0.3f
private const val LABEL_BACKGROUND_ALPHA = 0.1f
private const val LABEL_BORDER_ALPHA = 0.3f
private const val NOTE_MAX_LINES = 3

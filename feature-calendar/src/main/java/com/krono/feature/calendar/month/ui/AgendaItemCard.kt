package com.krono.feature.calendar.month.ui

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.R
import com.krono.feature.calendar.month.AgendaItemStatus
import com.krono.feature.calendar.month.AgendaItemUi

/**
 * Tarjeta de una tarea en la agenda del día (F-16 CA-2): horario de inicio a fin con el punto de
 * su categoría, estado ("En curso" o "Completada"), título, nota y etiquetas de categoría y
 * prioridad. Tocarla abre el detalle de la tarea.
 *
 * Sin ubicación (no existe en el modelo) ni botón de borrar (el calendario solo muestra tareas,
 * D-14). La fila del horario es la que recibirá el aviso de conflicto de F-17.
 *
 * @param item Tarea a mostrar.
 * @param onClick Abre el detalle de la tarea.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AgendaItemCard(
    item: AgendaItemUi,
    onClick: () -> Unit,
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
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            TimeRange(item)
            StatusBadge(item.status)
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
    val (textRes, color) = when (status) {
        AgendaItemStatus.IN_PROGRESS -> R.string.calendar_status_in_progress to PrimaryFixedDim
        AgendaItemStatus.COMPLETED -> R.string.calendar_status_completed to SecondaryCyan
        AgendaItemStatus.SCHEDULED -> return
    }
    TaskLabel(text = stringResource(textRes), color = color)
}

@Composable
private fun TaskLabel(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier
            .background(color.copy(alpha = LABEL_BACKGROUND_ALPHA), KronoShapes.extraSmall)
            .border(KronoGlass.borderWidth, color.copy(alpha = LABEL_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
        style = MaterialTheme.typography.labelMedium,
        color = color,
    )
}

private const val IN_PROGRESS_BORDER_ALPHA = 0.3f
private const val LABEL_BACKGROUND_ALPHA = 0.1f
private const val LABEL_BORDER_ALPHA = 0.3f
private const val NOTE_MAX_LINES = 3

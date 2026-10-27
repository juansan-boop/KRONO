package com.krono.feature.tasks.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/** Líneas máximas del título de cada tarea de la secuencia antes de cortarlo con "…". */
private const val SEQUENCE_TITLE_MAX_LINES = 2

/**
 * Encabezado de la secuencia sugerida para después de la tarea recomendada (F-12 CA-1).
 *
 * @param remaining cantidad de tareas en la secuencia.
 */
@Composable
internal fun SequenceHeader(remaining: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.assistant_sequence_title),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
            AssistantBadge(
                text = pluralStringResource(R.plurals.assistant_sequence_remaining, remaining, remaining),
                tint = OnSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.assistant_sequence_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

/**
 * Una tarea de la secuencia: posición, título, categoría con hora de inicio y duración.
 *
 * @param position posición en la secuencia, empezando en 1.
 * @param onOpenTask abre el detalle de la tarea.
 */
@Composable
internal fun SequenceItem(
    position: Int,
    planned: PlannedTask,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .clickable(role = Role.Button) { onOpenTask(planned.task.id) }
            .padding(KronoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PositionBubble(position)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
            Text(
                text = planned.task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
                maxLines = SEQUENCE_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.assistant_sequence_item_detail,
                    stringResource(planned.task.category.labelRes()),
                    clockText(planned.start),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        AssistantBadge(text = durationText(planned.estimate), tint = SecondaryCyan)
    }
}

@Composable
private fun PositionBubble(position: Int) {
    Box(
        modifier = Modifier
            .size(KronoSizes.iconLarge)
            .clip(CircleShape)
            .background(SurfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = position.toString(), style = MaterialTheme.typography.labelLarge, color = OnSurface)
    }
}

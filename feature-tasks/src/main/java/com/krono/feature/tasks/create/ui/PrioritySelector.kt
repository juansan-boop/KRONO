package com.krono.feature.tasks.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R

/**
 * Selector segmentado del nivel de prioridad (Baja, Media, Alta) para F-04.
 *
 * @param selectedPriority Prioridad actualmente seleccionada.
 * @param onPrioritySelected Callback invocado al seleccionar una prioridad.
 * @param modifier Modificador de diseño.
 */
@Composable
fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_flag),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(R.string.label_priority),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(KronoSpacing.xs))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KronoSpacing.md))
                .background(SurfaceContainerLow)
                .padding(KronoSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            TaskPriority.entries.forEach { priority ->
                val isSelected = priority == selectedPriority
                PrioritySegmentButton(
                    priority = priority,
                    isSelected = isSelected,
                    onClick = { onPrioritySelected(priority) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PrioritySegmentButton(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KronoSpacing.sm)
    val background = if (isSelected) {
        PrimaryMagenta.copy(alpha = SELECTED_BG_ALPHA)
    } else {
        SurfaceContainerHigh
    }
    val borderColor = if (isSelected) PrimaryMagenta else OutlineVariant
    val textColor = if (isSelected) OnSurface else OnSurfaceVariant

    val labelRes = when (priority) {
        TaskPriority.BAJA -> R.string.priority_low
        TaskPriority.MEDIA -> R.string.priority_medium
        TaskPriority.ALTA -> R.string.priority_high
    }

    Box(
        modifier = modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(shape)
            .background(background)
            .border(
                width = KronoSpacing.xs / 4,
                color = borderColor,
                shape = shape,
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            PrioritySignalBars(priority = priority, isSelected = isSelected)
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun PrioritySignalBars(
    priority: TaskPriority,
    isSelected: Boolean,
) {
    val activeColor = if (isSelected) PrimaryFixedDim else OnSurfaceVariant
    val inactiveColor = OutlineVariant.copy(alpha = INACTIVE_BAR_ALPHA)

    val activeCount = when (priority) {
        TaskPriority.BAJA -> 1
        TaskPriority.MEDIA -> 2
        TaskPriority.ALTA -> 3
    }

    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
    ) {
        val barWidth = KronoSpacing.xs / 2
        Box(
            modifier = Modifier
                .size(width = barWidth, height = KronoSpacing.sm)
                .clip(RoundedCornerShape(barWidth))
                .background(if (activeCount >= 1) activeColor else inactiveColor),
        )
        Box(
            modifier = Modifier
                .size(width = barWidth, height = KronoSpacing.sm + (KronoSpacing.xs / 2))
                .clip(RoundedCornerShape(barWidth))
                .background(if (activeCount >= 2) activeColor else inactiveColor),
        )
        Box(
            modifier = Modifier
                .size(width = barWidth, height = KronoSpacing.md)
                .clip(RoundedCornerShape(barWidth))
                .background(if (activeCount >= 3) activeColor else inactiveColor),
        )
    }
}

private const val SELECTED_BG_ALPHA = 0.25f
private const val INACTIVE_BAR_ALPHA = 0.4f

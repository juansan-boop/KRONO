package com.krono.feature.tasks.detail.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.krono.core.domain.task.Subtask
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.OnPrimaryMagenta
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Sección de subtareas con contador de progreso (x/n), barra visual interactiva y botón de añadir paso (F-06 CA-1, CA-2).
 *
 * @param subtasks Lista de subtareas embebidas.
 * @param completedCount Cantidad de pasos completados.
 * @param totalCount Cantidad total de pasos.
 * @param progressRatio Razón entre 0 y 1 del progreso.
 * @param onToggleSubtask Callback para conmutar el estado `isDone` de un paso.
 * @param onOpenAddStepDialog Callback para desplegar el diálogo de añadir paso.
 * @param modifier Modificador de diseño.
 */
@Composable
fun SubtaskSection(
    subtasks: List<Subtask>,
    completedCount: Int,
    totalCount: Int,
    progressRatio: Float,
    onToggleSubtask: (String) -> Unit,
    onOpenAddStepDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        // Cabecera de la sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.Checklist),
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Text(
                    text = stringResource(R.string.task_detail_subtasks_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                )
            }

            // Contador de progreso (x/n)
            Text(
                text = stringResource(R.string.task_detail_subtasks_counter, completedCount, totalCount),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
        }

        // Barra de progreso de subtareas
        LinearProgressIndicator(
            progress = { progressRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(KronoSpacing.sm)
                .clip(KronoShapes.extraSmall),
            color = PrimaryMagenta,
            trackColor = SurfaceContainerHigh,
        )

        Spacer(modifier = Modifier.height(KronoSpacing.xs))

        // Lista de subtareas
        subtasks.forEach { subtask ->
            SubtaskItemRow(
                subtask = subtask,
                onToggle = { onToggleSubtask(subtask.id) },
            )
        }

        // Botón "Añadir paso"
        OutlinedButton(
            onClick = onOpenAddStepDialog,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
            shape = KronoShapes.small,
            border = BorderStroke(KronoGlass.borderWidth, OutlineVariant),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(R.string.task_detail_add_step),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface,
            )
        }
    }
}

/**
 * Fila individual interactiva para una subtarea.
 */
@Composable
private fun SubtaskItemRow(
    subtask: Subtask,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cdAction = if (subtask.isDone) {
        stringResource(R.string.cd_subtask_checked, subtask.title)
    } else {
        stringResource(R.string.cd_subtask_checkbox, subtask.title)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        // Checkbox táctil (touch target accesible)
        Box(
            modifier = Modifier
                .size(KronoSizes.minTouchTarget),
            contentAlignment = Alignment.Center,
        ) {
            val checkSize = KronoSizes.iconMedium
            val checkBg = if (subtask.isDone) PrimaryMagenta else Color.Transparent
            val checkBorder = if (subtask.isDone) PrimaryMagenta else Outline

            Box(
                modifier = Modifier
                    .size(checkSize)
                    .clip(CircleShape)
                    .background(checkBg)
                    .border(width = KronoGlass.borderWidth, color = checkBorder, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (subtask.isDone) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = cdAction,
                        tint = OnPrimaryMagenta,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                }
            }
        }

        // Título del paso
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.isDone) OnSurfaceVariant.copy(alpha = COMPLETED_ALPHA) else OnSurface,
            textDecoration = if (subtask.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )
    }
}

private const val COMPLETED_ALPHA = 0.5f

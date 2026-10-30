package com.krono.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.OnPrimaryMagenta
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalLayoutApi::class)
/**
 * Tarjeta individual de tarea que visualiza sus atributos y permite completarla o eliminarla (F-03 CA-1, CA-3).
 */
@Composable
fun TaskCard(
    task: Task,
    today: LocalDate,
    onToggleCompletion: (Task) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val borderColor = when {
        task.priority == TaskPriority.ALTA -> PrimaryMagenta.copy(alpha = HIGH_PRIORITY_BORDER_ALPHA)
        task.category == TaskCategory.PERSONAL -> SecondaryCyan.copy(alpha = SECONDARY_BORDER_ALPHA)
        else -> OutlineVariant
    }

    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Column(
        modifier = cardModifier
            .fillMaxWidth()
            .glassSurface(borderColor = borderColor)
            .padding(KronoSpacing.md),
    ) {
        // Fila superior: Categoría y Prioridad; con fuente grande la segunda pasa a otra línea
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            CategoryBadge(category = task.category)
            PriorityBadge(priority = task.priority)
        }

        Spacer(modifier = Modifier.height(KronoSpacing.sm))

        // Título de la tarea (con tachado si está completa)
        val titleAlpha = if (task.isCompleted) COMPLETED_TITLE_ALPHA else 1f
        val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface.copy(alpha = titleAlpha),
            textDecoration = textDecoration,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(KronoSpacing.sm))

        // Fila inferior: fecha y estimación a la izquierda, acciones a la derecha (debajo con fuente grande)
        TrailingRowOrStack(
            modifier = Modifier.fillMaxWidth(),
            stackedTrailingAtEnd = true,
        ) {
            DueDateAndEstimate(task = task, today = today)
            TaskActions(task = task, onToggleCompletion = onToggleCompletion, onDelete = onDelete)
        }
    }
}

/** Fecha de entrega y, si existe, la estimación; la estimación pasa a otra línea si no cabe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DueDateAndEstimate(task: Task, today: LocalDate) {
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = OnSurfaceVariant,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = TaskDateFormatter.formatDueDate(context, task.dueDate, today),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
        TaskDateFormatter.formatEstimatedDuration(context, task.estimatedDuration)
            ?.let { EstimatedDurationBadge(text = it) }
    }
}

/** Acciones de la tarjeta: eliminar y completar, de al menos 48 dp. */
@Composable
private fun TaskActions(task: Task, onToggleCompletion: (Task) -> Unit, onDelete: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onDelete(task.id) },
            modifier = Modifier.size(KronoSizes.minTouchTarget),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.cd_delete_task, task.title),
                tint = Outline,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
        }

        TaskCheckbox(
            isCompleted = task.isCompleted,
            onClick = { onToggleCompletion(task) },
        )
    }
}

@Composable
private fun CategoryBadge(category: TaskCategory) {
    val (iconRes, textRes, tintColor) = when (category) {
        TaskCategory.ESTUDIO -> Triple(R.drawable.ic_school, R.string.filter_study, PrimaryFixedDim)
        TaskCategory.TRABAJO -> Triple(R.drawable.ic_work, R.string.filter_work, PrimaryFixedDim)
        TaskCategory.PERSONAL -> Triple(R.drawable.ic_person, R.string.filter_personal, SecondaryCyan)
    }
    val badgeShape = RoundedCornerShape(KronoSpacing.sm)

    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(tintColor.copy(alpha = BADGE_ALPHA))
            .border(width = KronoSpacing.xs / 4, color = tintColor.copy(alpha = BADGE_BORDER_ALPHA), shape = badgeShape)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(KronoSpacing.xs))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
        )
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val (textRes, tintColor) = when (priority) {
        TaskPriority.ALTA -> Pair(R.string.priority_high, PrimaryFixedDim)
        TaskPriority.MEDIA -> Pair(R.string.priority_medium, PrimaryFixedDim)
        TaskPriority.BAJA -> Pair(R.string.priority_low, OnSurfaceVariant)
    }
    val badgeShape = RoundedCornerShape(KronoSizes.minTouchTarget)

    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(tintColor.copy(alpha = BADGE_ALPHA))
            .border(width = KronoSpacing.xs / 4, color = tintColor.copy(alpha = BADGE_BORDER_ALPHA), shape = badgeShape)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
        )
    }
}

@Composable
private fun EstimatedDurationBadge(text: String) {
    val badgeShape = RoundedCornerShape(KronoSizes.minTouchTarget)
    Row(
        modifier = Modifier
            .clip(badgeShape)
            .background(PrimaryMagenta.copy(alpha = BADGE_ALPHA))
            .border(width = KronoSpacing.xs / 4, color = PrimaryMagenta.copy(alpha = BADGE_BORDER_ALPHA), shape = badgeShape)
            .padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_auto_awesome),
            contentDescription = null,
            tint = PrimaryFixedDim,
            modifier = Modifier.size(KronoSizes.iconSmall / 1.2f),
        )
        Spacer(modifier = Modifier.width(KronoSpacing.xs / 2))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryFixedDim,
        )
    }
}

@Composable
private fun TaskCheckbox(
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cdAction = if (isCompleted) {
        stringResource(R.string.action_mark_incomplete)
    } else {
        stringResource(R.string.action_mark_completed)
    }

    Box(
        modifier = modifier
            .size(KronoSizes.minTouchTarget)
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val checkShape = CircleShape
        val checkSize = KronoSizes.iconMedium
        val checkBg = if (isCompleted) PrimaryMagenta else Color.Transparent
        val checkBorder = if (isCompleted) PrimaryMagenta else Outline

        Box(
            modifier = Modifier
                .size(checkSize)
                .clip(checkShape)
                .background(checkBg)
                .border(width = KronoSpacing.xs / 2, color = checkBorder, shape = checkShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Icon(
                    painter = painterResource(R.drawable.ic_auto_awesome), // o check si disponible
                    contentDescription = cdAction,
                    tint = OnPrimaryMagenta,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
            }
        }
    }
}

private const val HIGH_PRIORITY_BORDER_ALPHA = 0.5f
private const val SECONDARY_BORDER_ALPHA = 0.4f
private const val BADGE_ALPHA = 0.12f
private const val BADGE_BORDER_ALPHA = 0.35f
private const val COMPLETED_TITLE_ALPHA = 0.5f

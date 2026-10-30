package com.krono.feature.dashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.dashboard.DashboardUiState
import com.krono.feature.dashboard.PriorityTaskUi
import com.krono.feature.dashboard.R

/**
 * Sección "Tareas prioritarias" (F-13 CA-3): título con "Ver todas" y las pendientes de hoy en
 * orden de prioridad y hora. Sin tareas hoy muestra el estado vacío; con todas completadas, un
 * mensaje de logro.
 *
 * @param onSeeAll Abre la lista completa de tareas.
 * @param onOpenTask Abre el detalle de una tarea por su identificador.
 */
@Composable
internal fun PriorityTasksSection(
    content: DashboardUiState.Content,
    onSeeAll: () -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
        PriorityTasksHeader(onSeeAll)
        when {
            !content.hasTasksToday -> MessageCard(R.string.dashboard_empty_title, R.string.dashboard_empty_body)
            content.priorityTasks.isEmpty() ->
                MessageCard(R.string.dashboard_all_done_title, R.string.dashboard_all_done_body)
            else -> content.priorityTasks.forEach { task ->
                PriorityTaskCard(task = task, onClick = { onOpenTask(task.taskId) })
            }
        }
    }
}

@Composable
private fun PriorityTasksHeader(onSeeAll: () -> Unit) {
    val seeAllDescription = stringResource(R.string.dashboard_see_all_description)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_priority_title),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        TextButton(
            onClick = onSeeAll,
            modifier = Modifier
                .heightIn(min = KronoSizes.minTouchTarget)
                .semantics { contentDescription = seeAllDescription },
        ) {
            Text(
                text = stringResource(R.string.dashboard_see_all),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryFixedDim,
            )
        }
    }
}

@Composable
private fun PriorityTaskCard(task: PriorityTaskUi, onClick: () -> Unit) {
    val accent = task.category.accentColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.minTouchTarget)
            .glassSurface()
            .clickable(onClickLabel = stringResource(R.string.dashboard_open_task), onClick = onClick)
            .padding(KronoSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Decorativo: el acento de color repite la categoría que ya dice el texto.
        Box(modifier = Modifier.size(KronoSizes.calendarCategoryDot).background(accent, CircleShape))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = taskDetailLine(task),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
        }
        PriorityBadge(text = stringResource(task.priority.labelRes), color = accent)
    }
}

@Composable
private fun taskDetailLine(task: PriorityTaskUi): String {
    val category = stringResource(task.category.labelRes)
    val time = task.dueTime ?: return category
    return stringResource(R.string.dashboard_task_time_category, clockTimeText(time), category)
}

@Composable
private fun PriorityBadge(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = BADGE_BACKGROUND_ALPHA), KronoShapes.extraSmall)
            .border(KronoGlass.borderWidth, color.copy(alpha = BADGE_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
    )
}

private const val TITLE_MAX_LINES = 2
private const val BADGE_BACKGROUND_ALPHA = 0.1f
private const val BADGE_BORDER_ALPHA = 0.3f

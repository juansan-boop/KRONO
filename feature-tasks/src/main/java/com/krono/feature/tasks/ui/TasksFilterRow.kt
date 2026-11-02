package com.krono.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.OnPrimaryMagenta
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainer
import com.krono.feature.tasks.R
import com.krono.feature.tasks.TaskCategoryFilter

/**
 * Fila horizontal deslizable con chips de filtrado por categoría (F-03 CA-2).
 */
@Composable
fun TasksFilterRow(
    selectedCategory: TaskCategoryFilter,
    categoryCounts: Map<TaskCategoryFilter, Int>,
    onSelectCategory: (TaskCategoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = KronoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCategoryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedCategory
            val count = categoryCounts[filter] ?: 0

            CategoryFilterChip(
                filter = filter,
                count = count,
                isSelected = isSelected,
                onClick = { onSelectCategory(filter) },
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    filter: TaskCategoryFilter,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipShape = RoundedCornerShape(KronoSizes.minTouchTarget)
    val backgroundColor = if (isSelected) PrimaryMagenta else SurfaceContainer
    val contentColor = if (isSelected) OnPrimaryMagenta else OnSurfaceVariant
    val borderColor = if (isSelected) PrimaryMagenta else OutlineVariant

    Row(
        modifier = modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(chipShape)
            .background(backgroundColor)
            .border(width = KronoSpacing.xs / 4, color = borderColor, shape = chipShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(getFilterIcon(filter)),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Spacer(modifier = Modifier.width(KronoSpacing.xs))
        Text(
            text = stringResource(getFilterLabel(filter)),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
        )
        Spacer(modifier = Modifier.width(KronoSpacing.xs))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(KronoSizes.iconSmall))
                .background(contentColor.copy(alpha = BADGE_ALPHA))
                .padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.xs / 2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

private fun getFilterLabel(filter: TaskCategoryFilter): Int = when (filter) {
    TaskCategoryFilter.TODAS -> R.string.filter_all
    TaskCategoryFilter.ESTUDIO -> R.string.filter_study
    TaskCategoryFilter.TRABAJO -> R.string.filter_work
    TaskCategoryFilter.PERSONAL -> R.string.filter_personal
}

private fun getFilterIcon(filter: TaskCategoryFilter): Int = when (filter) {
    TaskCategoryFilter.TODAS -> R.drawable.ic_layers
    TaskCategoryFilter.ESTUDIO -> R.drawable.ic_school
    TaskCategoryFilter.TRABAJO -> R.drawable.ic_work
    TaskCategoryFilter.PERSONAL -> R.drawable.ic_person
}

private const val BADGE_ALPHA = 0.2f

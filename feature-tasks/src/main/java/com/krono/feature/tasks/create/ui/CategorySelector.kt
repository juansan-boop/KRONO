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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.krono.core.domain.task.TaskCategory
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R

/**
 * Selector segmentado de categoría de tarea en 3 opciones (Estudio, Trabajo, Personal) para F-04.
 *
 * @param selectedCategory Categoría actualmente seleccionada.
 * @param onCategorySelected Callback invocado al seleccionar una categoría.
 * @param modifier Modificador de diseño.
 */
@Composable
fun CategorySelector(
    selectedCategory: TaskCategory,
    onCategorySelected: (TaskCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryLabel = when (selectedCategory) {
        TaskCategory.ESTUDIO -> stringResource(R.string.category_study)
        TaskCategory.TRABAJO -> stringResource(R.string.category_work)
        TaskCategory.PERSONAL -> stringResource(R.string.category_personal)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_layers),
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(KronoSpacing.xs))
                Text(
                    text = stringResource(R.string.label_category),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.category_selected_format, categoryLabel),
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryFixedDim,
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
            TaskCategory.entries.forEach { category ->
                val isSelected = category == selectedCategory
                CategorySegmentButton(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CategorySegmentButton(
    category: TaskCategory,
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
    val iconTint = if (isSelected) PrimaryFixedDim else OnSurfaceVariant

    val (labelRes, iconRes) = when (category) {
        TaskCategory.ESTUDIO -> R.string.category_study to R.drawable.ic_school
        TaskCategory.TRABAJO -> R.string.category_work to R.drawable.ic_work
        TaskCategory.PERSONAL -> R.string.category_personal to R.drawable.ic_person
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
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}

private const val SELECTED_BG_ALPHA = 0.25f

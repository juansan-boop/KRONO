package com.krono.feature.calendar.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.calendar.R

/** Orden de las pestañas del selector, como en el diseño. */
private val ViewModeOrder = listOf(CalendarViewMode.MONTH, CalendarViewMode.WEEK, CalendarViewMode.DAY)

@get:StringRes
private val CalendarViewMode.labelRes: Int
    get() = when (this) {
        CalendarViewMode.MONTH -> R.string.calendar_view_month
        CalendarViewMode.WEEK -> R.string.calendar_view_week
        CalendarViewMode.DAY -> R.string.calendar_view_day
    }

/**
 * Selector de vista Mes / Semana / Día (F-16 CA-1): tres pestañas de 48 dp de alto, una por vista.
 *
 * @param selectedMode Vista activa.
 * @param onSelectMode Cambia a la vista tocada.
 */
@Composable
internal fun CalendarViewSelector(
    selectedMode: CalendarViewMode,
    onSelectMode: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectorDescription = stringResource(R.string.calendar_view_selector)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(KronoShapes.extraSmall)
            .background(SurfaceContainerLow)
            .border(KronoGlass.borderWidth, OutlineVariant.copy(alpha = SELECTOR_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(KronoSpacing.xs)
            .semantics { contentDescription = selectorDescription }
            .selectableGroup(),
    ) {
        ViewModeOrder.forEach { mode ->
            ViewModeTab(mode = mode, isSelected = mode == selectedMode, onClick = { onSelectMode(mode) })
        }
    }
}

@Composable
private fun RowScope.ViewModeTab(mode: CalendarViewMode, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(KronoShapes.extraSmall)
            .background(if (isSelected) PrimaryFixedDim.copy(alpha = SELECTED_TAB_ALPHA) else SurfaceContainerLow)
            .selectable(selected = isSelected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(mode.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) PrimaryFixedDim else OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val SELECTOR_BORDER_ALPHA = 0.2f
private const val SELECTED_TAB_ALPHA = 0.2f

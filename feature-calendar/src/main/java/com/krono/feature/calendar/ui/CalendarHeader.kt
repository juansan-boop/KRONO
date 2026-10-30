package com.krono.feature.calendar.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.krono.core.domain.calendar.CalendarViewMode
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.feature.calendar.R

/**
 * Encabezado del calendario, igual en las tres vistas: navegación al período anterior y
 * siguiente con su título, botón "Hoy" y selector de vista.
 *
 * @param mode Vista activa; decide las descripciones de los botones de navegación.
 * @param title Título de la vista, con su línea superior opcional.
 * @param onPrevious Muestra el período anterior.
 * @param onNext Muestra el período siguiente.
 * @param onToday Vuelve al período y al día actuales.
 * @param onSelectMode Cambia de vista.
 */
@Composable
internal fun CalendarHeader(
    mode: CalendarViewMode,
    title: CalendarTitle,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectMode: (CalendarViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PeriodStepButton(
                icon = KronoIcons.ChevronLeft,
                description = previousPeriodDescription(mode),
                onClick = onPrevious,
            )
            PeriodTitle(title = title, modifier = Modifier.weight(1f))
            PeriodStepButton(
                icon = KronoIcons.ChevronRight,
                description = nextPeriodDescription(mode),
                onClick = onNext,
            )
            TodayButton(onClick = onToday)
        }
        CalendarViewSelector(selectedMode = mode, onSelectMode = onSelectMode)
    }
}

@Composable
private fun PeriodTitle(title: CalendarTitle, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        title.eyebrow?.let { eyebrow ->
            Text(text = eyebrow, style = MaterialTheme.typography.labelMedium, color = SecondaryCyan)
        }
        Text(
            text = title.title,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
        )
    }
}

@Composable
private fun PeriodStepButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    // Tamaño explícito: el IconButton de M3 mide 40dp y solo amplía la zona táctil por fuera.
    IconButton(onClick = onClick, modifier = Modifier.size(KronoSizes.minTouchTarget)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = OnSurfaceVariant,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
    }
}

@Composable
private fun TodayButton(onClick: () -> Unit) {
    val description = stringResource(R.string.calendar_today_description)
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .semantics { contentDescription = description },
        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryFixedDim),
    ) {
        Text(text = stringResource(R.string.calendar_today), style = MaterialTheme.typography.labelLarge)
    }
}

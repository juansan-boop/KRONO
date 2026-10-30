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
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDate

/**
 * Encabezado de la vista mensual: navegación entre meses, botón "Hoy" y selector de vista.
 *
 * @param displayedMonth Cualquier fecha del mes mostrado.
 * @param onPreviousMonth Muestra el mes anterior.
 * @param onNextMonth Muestra el mes siguiente.
 * @param onToday Vuelve al mes y al día actuales.
 */
@Composable
internal fun CalendarHeader(
    displayedMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonthStepButton(
                icon = KronoIcons.ChevronLeft,
                description = stringResource(R.string.calendar_previous_month),
                onClick = onPreviousMonth,
            )
            Text(
                text = monthTitle(displayedMonth),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
            MonthStepButton(
                icon = KronoIcons.ChevronRight,
                description = stringResource(R.string.calendar_next_month),
                onClick = onNextMonth,
            )
            TodayButton(onClick = onToday)
        }
        CalendarViewSelector()
    }
}

@Composable
private fun MonthStepButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
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

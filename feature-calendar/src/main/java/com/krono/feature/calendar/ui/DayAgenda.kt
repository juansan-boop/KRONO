package com.krono.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.Tertiary
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.DayAgendaState
import com.krono.feature.calendar.NowMarker
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDate

/**
 * Añade a la lista la agenda del día seleccionado (F-16 CA-2 y CA-3): encabezado con el día y el
 * número de tareas, y debajo las tarjetas, el estado vacío, la carga o el error.
 *
 * @param selectedDate Día cuya agenda se muestra.
 * @param agenda Estado de la agenda.
 * @param onOpenTask Abre el detalle de una tarea por su identificador.
 * @param onRetry Vuelve a leer las tareas tras un error.
 * @param itemModifier Modificador que se aplica a cada elemento de la agenda (su margen lateral).
 */
internal fun LazyListScope.dayAgenda(
    selectedDate: LocalDate,
    agenda: DayAgendaState,
    onOpenTask: (String) -> Unit,
    onRetry: () -> Unit,
    itemModifier: Modifier = Modifier,
) {
    item(key = AGENDA_HEADER_KEY) {
        AgendaHeader(
            selectedDate = selectedDate,
            taskCount = (agenda as? DayAgendaState.Loaded)?.items?.size,
            modifier = itemModifier,
        )
    }
    when (agenda) {
        DayAgendaState.Loading -> item(key = AGENDA_STATUS_KEY) { AgendaLoading(itemModifier) }
        DayAgendaState.LoadError -> item(key = AGENDA_STATUS_KEY) {
            CalendarLoadErrorCard(onRetry = onRetry, modifier = itemModifier)
        }
        is DayAgendaState.Loaded -> if (agenda.items.isEmpty()) {
            item(key = AGENDA_STATUS_KEY) { AgendaEmptyState(itemModifier) }
        } else {
            agendaItems(loaded = agenda, onOpenTask = onOpenTask, itemModifier = itemModifier)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgendaHeader(selectedDate: LocalDate, taskCount: Int?, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = KronoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Text(
            text = stringResource(R.string.calendar_agenda_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
        )
        Text(
            text = agendaDayLabel(selectedDate),
            modifier = Modifier
                .background(SecondaryCyan.copy(alpha = DAY_CHIP_ALPHA), KronoShapes.small)
                .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
            style = MaterialTheme.typography.labelMedium,
            color = SecondaryCyan,
        )
        taskCount?.takeIf { it > 0 }?.let { count ->
            Text(
                text = pluralStringResource(R.plurals.calendar_agenda_count, count, count),
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AgendaLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.calendar_agenda_loading)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(KronoSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics { contentDescription = description },
            color = PrimaryFixedDim,
            strokeWidth = KronoSizes.progressStroke,
        )
    }
}

@Composable
private fun AgendaEmptyState(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.calendar_agenda_empty),
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.lg),
        style = MaterialTheme.typography.bodyLarge,
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private const val AGENDA_HEADER_KEY = "agenda-header"
private const val AGENDA_STATUS_KEY = "agenda-status"
private const val DAY_CHIP_ALPHA = 0.2f

/** Tarjetas de la agenda, con el separador "Ahora" de la vista diaria en su sitio si lo hay. */
private fun LazyListScope.agendaItems(
    loaded: DayAgendaState.Loaded,
    onOpenTask: (String) -> Unit,
    itemModifier: Modifier,
) {
    val marker = loaded.nowMarker
    loaded.items.forEachIndexed { index, item ->
        if (marker?.beforeIndex == index) item(key = NOW_MARKER_KEY) { NowMarkerRow(marker, itemModifier) }
        item(key = item.taskId) {
            AgendaItemCard(item = item, onClick = { onOpenTask(item.taskId) }, modifier = itemModifier)
        }
    }
    // Si todas las tareas ya terminaron, la hora actual queda después de la última.
    if (marker != null && marker.beforeIndex >= loaded.items.size) {
        item(key = NOW_MARKER_KEY) { NowMarkerRow(marker, itemModifier) }
    }
}

/** Separador "Ahora · 14:15": texto, línea que se desvanece y punto, en el rosa de la paleta. */
@Composable
private fun NowMarkerRow(marker: NowMarker, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = nowMarkerLabel(marker.time),
            style = MaterialTheme.typography.labelMedium,
            color = Tertiary,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(KronoGlass.borderWidth)
                .background(Brush.horizontalGradient(listOf(Tertiary, Color.Transparent))),
        )
        Box(modifier = Modifier.size(KronoSizes.calendarCategoryDot).background(Tertiary, CircleShape))
    }
}

private const val NOW_MARKER_KEY = "now-marker"

package com.krono.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHighest
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.dashboard.DashboardUiState
import com.krono.feature.dashboard.R

private const val PERCENT_BASE = 100f

/**
 * Tarjeta "Resumen de hoy" (F-13 CA-1): porcentaje del día completado con su barra y los conteos
 * de completadas, pendientes y el tiempo de enfoque.
 */
@Composable
internal fun DaySummaryCard(content: DashboardUiState.Content, modifier: Modifier = Modifier) {
    val progressDescription = stringResource(R.string.dashboard_summary_progress_description, content.completedPercent)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        Text(
            text = stringResource(R.string.dashboard_summary_title),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
        )
        Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
            Text(
                text = stringResource(R.string.dashboard_summary_percent, content.completedPercent),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryFixedDim,
            )
            LinearProgressIndicator(
                progress = { content.completedPercent / PERCENT_BASE },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KronoSpacing.sm)
                    .clip(KronoShapes.extraSmall)
                    .semantics { contentDescription = progressDescription },
                color = PrimaryFixedDim,
                trackColor = SurfaceContainerHighest,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
            SummaryStat(
                value = content.completedCount.toString(),
                label = stringResource(R.string.dashboard_stat_completed),
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                value = content.pendingCount.toString(),
                label = stringResource(R.string.dashboard_stat_pending),
                modifier = Modifier.weight(1f),
            )
            SummaryStat(
                value = focusTimeText(content.focusTime),
                label = stringResource(R.string.dashboard_stat_focus),
                modifier = Modifier.weight(1f),
                valueColor = SecondaryCyan,
            )
        }
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurface,
) {
    val description = stringResource(R.string.dashboard_stat_description, label, value)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

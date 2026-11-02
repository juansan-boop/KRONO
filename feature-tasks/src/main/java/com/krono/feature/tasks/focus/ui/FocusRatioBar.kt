package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R

/**
 * Barra del ciclo con el porcentaje de foco y de descanso de una plantilla (F-10 CA-3), y debajo
 * sus dos etiquetas. Las etiquetas pasan a dos líneas, sin partir palabras, si no caben juntas.
 *
 * @param focusPercent porcentaje del ciclo dedicado al foco.
 * @param breakPercent porcentaje del ciclo dedicado al descanso.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusRatioBar(
    focusPercent: Int,
    breakPercent: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        // La barra es decorativa: las etiquetas de abajo ya dicen los mismos porcentajes.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT)
                .clip(CircleShape)
                .background(SurfaceContainerLow)
                .clearAndSetSemantics { },
        ) {
            Box(Modifier.weight(focusPercent.coerceAtLeast(MIN_WEIGHT).toFloat()).fillMaxHeight().background(PrimaryMagenta))
            Box(
                Modifier
                    .weight(breakPercent.coerceAtLeast(MIN_WEIGHT).toFloat())
                    .fillMaxHeight()
                    .background(SecondaryCyan.copy(alpha = BREAK_ALPHA)),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Text(
                text = stringResource(R.string.focus_percent_focus, focusPercent),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.focus_percent_break, breakPercent),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
        }
    }
}

private val BAR_HEIGHT = KronoSpacing.sm - KronoSpacing.xs / 2
private const val MIN_WEIGHT = 1
private const val BREAK_ALPHA = 0.6f

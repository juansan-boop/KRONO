package com.krono.feature.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Tarjeta resumen del "Enfoque Diario" que muestra el avance porcentual y pendientes del día (F-03 CA-3).
 */
@Composable
fun DailyFocusCard(
    percentage: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier,
) {
    val cdProgress = stringResource(R.string.cd_focus_progress, percentage)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md)
            .semantics { contentDescription = cdProgress },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ícono de tendencia / progreso
        Box(
            modifier = Modifier
                .size(KronoSizes.iconLarge)
                .clip(RoundedCornerShape(KronoSpacing.sm))
                .background(SurfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trending_up),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }

        Spacer(modifier = Modifier.width(KronoSpacing.md))

        // Textos descriptivos
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.daily_focus_title),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
            val subtitleText = when {
                percentage == 100 && pendingCount == 0 ->
                    stringResource(R.string.daily_focus_all_done)
                percentage == 0 && pendingCount == 0 ->
                    stringResource(R.string.daily_focus_no_tasks)
                pendingCount == 1 ->
                    stringResource(R.string.daily_focus_single_pending)
                else ->
                    stringResource(R.string.daily_focus_subtitle, pendingCount)
            }
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(KronoSpacing.sm))

        // Badge con el porcentaje de completitud
        val badgeShape = RoundedCornerShape(KronoSizes.minTouchTarget)
        Row(
            modifier = Modifier
                .clip(badgeShape)
                .background(PrimaryMagenta.copy(alpha = BADGE_BG_ALPHA))
                .border(
                    width = KronoSpacing.xs / 4,
                    color = PrimaryMagenta.copy(alpha = BADGE_BORDER_ALPHA),
                    shape = badgeShape,
                )
                .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_trending_up),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Spacer(modifier = Modifier.width(KronoSpacing.xs))
            Text(
                text = stringResource(R.string.percentage_format, percentage),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryFixedDim,
            )
        }
    }
}

private const val BADGE_BG_ALPHA = 0.15f
private const val BADGE_BORDER_ALPHA = 0.35f

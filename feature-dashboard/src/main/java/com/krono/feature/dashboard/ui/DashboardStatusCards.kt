package com.krono.feature.dashboard.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.dashboard.R

/** Indicador de carga del inicio, anunciado a los lectores de pantalla. */
@Composable
internal fun DashboardLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.dashboard_loading)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = KronoSpacing.xl)
            .semantics {
                contentDescription = description
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PrimaryFixedDim)
    }
}

/**
 * Tarjeta de error del inicio con "Reintentar". Se muestra cuando no se pudieron leer las tareas
 * o las sesiones, en lugar de un resumen en ceros que sería falso. Nunca muestra el mensaje
 * técnico de Firebase.
 *
 * @param onRetry Vuelve a leer los datos del día.
 */
@Composable
internal fun DashboardErrorCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(borderColor = ErrorColor.copy(alpha = ERROR_BORDER_ALPHA))
            .padding(KronoSpacing.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(KronoSizes.minTouchTarget)
                    .background(ErrorContainer.copy(alpha = BADGE_CONTAINER_ALPHA), KronoShapes.small),
                contentAlignment = Alignment.Center,
            ) {
                // Decorativo: el título ya dice que hubo un error.
                Icon(
                    painter = painterResource(KronoIcons.Error),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Text(
                    text = stringResource(R.string.dashboard_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_error_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.minTouchTarget),
            shape = KronoShapes.small,
            border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = ERROR_BORDER_ALPHA)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ErrorContainer.copy(alpha = RETRY_CONTAINER_ALPHA),
                contentColor = ErrorColor,
            ),
        ) {
            Text(text = stringResource(R.string.dashboard_retry), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Tarjeta informativa con título y explicación, para el estado vacío y el de "todo completado". */
@Composable
internal fun MessageCard(
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Text(text = stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
    }
}

private const val ERROR_BORDER_ALPHA = 0.4f
private const val RETRY_CONTAINER_ALPHA = 0.25f
private const val BADGE_CONTAINER_ALPHA = 0.35f

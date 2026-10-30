package com.krono.feature.calendar.month.ui

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
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.calendar.R

/**
 * Tarjeta de error de carga del calendario con "Reintentar". Sigue el mismo patrón visual que la
 * tarjeta de error de la lista de tareas (vidrio con borde de error, insignia, título,
 * explicación y botón), sin importar `feature-tasks`.
 *
 * Se muestra cuando no se pudieron leer las tareas (por ejemplo, permiso denegado), en lugar de
 * una agenda vacía falsa. Nunca muestra el mensaje técnico de Firebase.
 *
 * @param onRetry Vuelve a abrir la observación de tareas.
 */
@Composable
internal fun CalendarLoadErrorCard(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            ErrorBadge()
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Text(
                    text = stringResource(R.string.calendar_load_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                )
                Text(
                    text = stringResource(R.string.calendar_load_error_body),
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
            Text(text = stringResource(R.string.calendar_retry), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ErrorBadge() {
    Box(
        modifier = Modifier
            .size(KronoSizes.minTouchTarget)
            .background(ErrorContainer.copy(alpha = BADGE_CONTAINER_ALPHA), KronoShapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(KronoIcons.Error),
            contentDescription = null,
            tint = ErrorColor,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
    }
}

private const val ERROR_BORDER_ALPHA = 0.4f
private const val RETRY_CONTAINER_ALPHA = 0.25f
private const val BADGE_CONTAINER_ALPHA = 0.35f

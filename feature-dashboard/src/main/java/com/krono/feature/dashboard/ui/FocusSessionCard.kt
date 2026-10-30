package com.krono.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnPrimaryMagenta
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.dashboard.R

/**
 * Bloque "Sesión de Enfoque" con el botón "Iniciar" (F-13 CA-2). La tarjeta no decide a dónde
 * lleva el botón: lo hace [onStart], que `app` conecta con la navegación.
 *
 * @param onStart Se invoca al tocar "Iniciar".
 */
@Composable
internal fun FocusSessionCard(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val startDescription = stringResource(R.string.dashboard_focus_start_description)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
            Text(
                text = stringResource(R.string.dashboard_focus_title),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
            Text(
                text = stringResource(R.string.dashboard_focus_body),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.minTouchTarget)
                .semantics { contentDescription = startDescription },
            shape = KronoShapes.small,
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryMagenta,
                contentColor = OnPrimaryMagenta,
            ),
        ) {
            Text(text = stringResource(R.string.dashboard_focus_start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

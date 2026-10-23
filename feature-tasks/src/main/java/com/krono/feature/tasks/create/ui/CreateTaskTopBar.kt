package com.krono.feature.tasks.create.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.krono.core.ui.brand.KronoSymbol
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.feature.tasks.R

/**
 * Barra superior de navegación para la pantalla de creación de tarea (F-04).
 *
 * @param onBack Callback invocado al pulsar el botón de volver.
 * @param modifier Modificador de diseño.
 */
@Composable
fun CreateTaskTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(KronoSizes.minTouchTarget),
        ) {
            Icon(
                painter = painterResource(KronoIcons.ArrowBack),
                contentDescription = stringResource(R.string.action_back),
                tint = OnSurface,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KronoSymbol(
                height = KronoSizes.brandSymbolSmall,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(KronoSpacing.sm))
            Text(
                text = stringResource(com.krono.core.ui.R.string.krono_wordmark),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
        }
    }
}

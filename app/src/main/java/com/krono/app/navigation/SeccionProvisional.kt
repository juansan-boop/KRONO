package com.krono.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.krono.app.R
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.glassSurface

/**
 * Pantalla provisional de una sección de la barra inferior: tarjeta glass con el nombre de
 * la sección y "Próximamente". Se reemplaza por el grafo de cada feature cuando se implemente.
 *
 * @param titulo nombre de la sección.
 * @param contenidoExtra contenido adicional bajo el texto "Próximamente" (p. ej. acciones temporales).
 */
@Composable
fun SeccionProvisional(
    @StringRes titulo: Int,
    modifier: Modifier = Modifier,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(KronoSpacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KronoSizes.formMaxWidth)
                .fillMaxWidth()
                .glassSurface()
                .padding(KronoSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(titulo),
                style = MaterialTheme.typography.headlineLarge,
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.seccion_proximamente),
                modifier = Modifier.padding(top = KronoSpacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            contenidoExtra()
        }
    }
}

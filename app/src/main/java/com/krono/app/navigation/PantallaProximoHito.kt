package com.krono.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
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
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface

/**
 * Pantalla provisional tras iniciar sesión o crear cuenta (F-22 a F-25 CA-5).
 * F-01 la reemplazará por el flujo principal con barra inferior.
 */
@Composable
fun PantallaProximoHito(modifier: Modifier = Modifier) {
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
            Icon(
                imageVector = Icons.Outlined.HourglassTop,
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconLarge),
            )
            Spacer(Modifier.height(KronoSpacing.md))
            Text(
                text = stringResource(R.string.proximo_hito),
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

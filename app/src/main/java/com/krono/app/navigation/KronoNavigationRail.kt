package com.krono.app.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.app.R
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface

/**
 * Riel de navegación lateral con efecto Lumina Glass para ventanas amplias (ancho >= 600.dp).
 * Provee objetivos táctiles accesibles (>= 48.dp) y descripciones semánticas completas para TalkBack.
 */
@Composable
fun KronoNavigationRail(
    seleccionado: KronoDestination?,
    onSeleccionar: (KronoDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .glassSurface(),
        containerColor = Color.Transparent,
        header = {
            Spacer(modifier = Modifier.height(KronoSpacing.lg))
        },
    ) {
        Spacer(modifier = Modifier.height(KronoSpacing.md))
        KronoDestination.entries.forEach { destino ->
            val etiqueta = stringResource(destino.labelRes)
            val descripcion = stringResource(R.string.nav_ir_a, etiqueta)
            NavigationRailItem(
                selected = destino == seleccionado,
                onClick = { onSeleccionar(destino) },
                modifier = Modifier
                    .padding(vertical = KronoSpacing.xs)
                    .semantics { contentDescription = descripcion },
                icon = {
                    Icon(
                        painter = painterResource(destino.icono),
                        contentDescription = null,
                    )
                },
                label = { EtiquetaDePestana(etiqueta) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = PrimaryFixedDim,
                    selectedTextColor = PrimaryFixedDim,
                    unselectedIconColor = Outline,
                    unselectedTextColor = Outline,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

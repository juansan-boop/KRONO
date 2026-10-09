package com.krono.app.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import com.krono.app.R
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface

/**
 * Escala máxima de fuente de las etiquetas de la barra. Cuatro etiquetas comparten el ancho
 * de la pantalla: al 200 % palabras como "Calendario" no caben en una línea y se partirían a
 * mitad de palabra. Los íconos y la descripción de TalkBack no dependen de este límite.
 */
private const val ESCALA_MAXIMA_ETIQUETA = 1.3f

@get:DrawableRes
private val KronoDestination.icono: Int
    get() = when (this) {
        KronoDestination.Inicio -> KronoIcons.GridView
        KronoDestination.Tareas -> KronoIcons.Checklist
        KronoDestination.Calendario -> KronoIcons.CalendarToday
        KronoDestination.Ajustes -> KronoIcons.Settings
    }

/** Texto de una pestaña: una sola línea y con la escala de fuente acotada a [ESCALA_MAXIMA_ETIQUETA]. */
@Composable
private fun EtiquetaDePestana(texto: String) {
    val densidad = LocalDensity.current
    val densidadAcotada = Density(densidad.density, densidad.fontScale.coerceAtMost(ESCALA_MAXIMA_ETIQUETA))
    CompositionLocalProvider(LocalDensity provides densidadAcotada) {
        Text(text = texto, maxLines = 1, softWrap = false)
    }
}

/** Barra inferior con efecto glass; las áreas táctiles de [NavigationBarItem] superan los 48dp. */
@Composable
fun KronoBottomBar(
    seleccionado: KronoDestination?,
    onSeleccionar: (KronoDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier.glassSurface(),
        containerColor = Color.Transparent,
    ) {
        KronoDestination.entries.forEach { destino ->
            val etiqueta = stringResource(destino.labelRes)
            val descripcion = stringResource(R.string.nav_ir_a, etiqueta)
            NavigationBarItem(
                selected = destino == seleccionado,
                onClick = { onSeleccionar(destino) },
                // La descripción va en el ítem y no en el ícono: con la etiqueta siempre visible,
                // NavigationBarItem borra la semántica del ícono y TalkBack no la anunciaría.
                modifier = Modifier.semantics { contentDescription = descripcion },
                icon = { Icon(painter = painterResource(destino.icono), contentDescription = null) },
                label = { EtiquetaDePestana(etiqueta) },
                colors = NavigationBarItemDefaults.colors(
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

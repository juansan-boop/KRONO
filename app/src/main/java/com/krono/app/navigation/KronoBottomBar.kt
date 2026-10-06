package com.krono.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.krono.app.R
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface

private val KronoDestination.icono: ImageVector
    get() = when (this) {
        KronoDestination.Inicio -> Icons.Outlined.GridView
        KronoDestination.Tareas -> Icons.Outlined.Checklist
        KronoDestination.Calendario -> Icons.Outlined.CalendarToday
        KronoDestination.Ajustes -> Icons.Outlined.Settings
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
            NavigationBarItem(
                selected = destino == seleccionado,
                onClick = { onSeleccionar(destino) },
                icon = {
                    Icon(
                        imageVector = destino.icono,
                        contentDescription = stringResource(R.string.nav_ir_a, etiqueta),
                    )
                },
                label = { Text(etiqueta) },
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

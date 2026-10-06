package com.krono.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.krono.app.R
import com.krono.core.ui.theme.DeepMidnight

/** Raíz de la UI: fondo Deep Midnight (core-ui), NavHost y barra inferior. */
@Composable
fun KronoApp() {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination
    val seleccionado = KronoDestination.entries.firstOrNull { destino ->
        destinoActual?.hierarchy?.any { it.route == destino.route } == true
    }

    Scaffold(
        containerColor = DeepMidnight,
        bottomBar = {
            // La barra solo aparece en el flujo principal (no en un futuro grafo de auth).
            if (seleccionado != null) {
                KronoBottomBar(
                    seleccionado = seleccionado,
                    onSeleccionar = { navController.navegarAPestana(it) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = RUTA_GRAFO_PRINCIPAL,
            modifier = Modifier.padding(padding),
        ) {
            // TODO(auth): agregar aquí el grafo de autenticación antes del flujo principal.
            grafoPrincipal()
        }
    }
}

private fun NavGraphBuilder.grafoPrincipal() {
    navigation(startDestination = KronoDestination.Inicial.route, route = RUTA_GRAFO_PRINCIPAL) {
        // TODO(feature-*): reemplazar cada pantalla provisional por el grafo que exponga
        // su feature (dashboard, tasks, calendar, profile) cuando se implemente.
        composable(KronoDestination.Inicio.route) { SeccionProvisional(R.string.seccion_inicio) }
        composable(KronoDestination.Tareas.route) { SeccionProvisional(R.string.seccion_tareas) }
        composable(KronoDestination.Calendario.route) { SeccionProvisional(R.string.seccion_calendario) }
        composable(KronoDestination.Ajustes.route) { SeccionProvisional(R.string.seccion_ajustes) }
    }
}

/** Cambia de pestaña conservando y restaurando el estado de la anterior (F-01 CA-2). */
fun NavController.navegarAPestana(destino: KronoDestination) {
    navigate(destino.route) {
        popUpTo(KronoDestination.Inicial.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

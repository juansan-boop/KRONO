package com.krono.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.krono.feature.auth.navigation.AUTH_GRAPH_ROUTE
import com.krono.feature.auth.navigation.authGraph

/**
 * Raíz de la UI: fondo Deep Midnight (core-ui), NavHost y barra inferior.
 * Sin sesión arranca en el grafo de auth; con sesión activa entra directo (CA-6).
 */
@Composable
fun KronoApp(viewModel: KronoAppViewModel = hiltViewModel()) {
    val inicio by viewModel.startDestination.collectAsState()
    if (inicio == StartDestination.Loading) {
        // Lectura breve de la sesión de Firebase: solo el fondo, sin parpadeo del login.
        Box(Modifier.fillMaxSize().background(DeepMidnight))
        return
    }
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
            startDestination = if (inicio == StartDestination.Home) RUTA_PROXIMO_HITO else AUTH_GRAPH_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            authGraph(
                navController = navController,
                onAuthSuccess = {
                    navController.navigate(RUTA_PROXIMO_HITO) {
                        // Tras autenticarse, "atrás" no vuelve al login.
                        popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                    }
                },
            )
            composable(RUTA_PROXIMO_HITO) {
                PantallaProximoHito(
                    // TEMPORAL: se elimina cuando existan F-01/F-19 (feature-profile)
                    alCerrarSesion = {
                        navController.navigate(AUTH_GRAPH_ROUTE) {
                            // "Atrás" no regresa a la pantalla provisional.
                            popUpTo(RUTA_PROXIMO_HITO) { inclusive = true }
                        }
                    },
                )
            }
            // F-01: flujo principal con barra inferior. Aún no es destino tras autenticarse (CA-5).
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

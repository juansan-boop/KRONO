package com.krono.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
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
 * Raíz de la UI: fondo Deep Midnight (core-ui) y [KronoShell]. Sin sesión arranca en el grafo
 * de auth; con sesión activa entra directo al flujo principal (F-22 a F-25 CA-6).
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
    KronoShell(
        navController = navController,
        destinoInicial = if (inicio == StartDestination.Home) RUTA_GRAFO_PRINCIPAL else AUTH_GRAPH_ROUTE,
        grafoAuth = { alAutenticarse -> authGraph(navController, alAutenticarse) },
        ajustes = { alCerrarSesion -> AjustesRoute(alCerrarSesion) },
    )
}

/**
 * Estructura de navegación de la app: [NavHost] con el grafo de auth y el flujo principal, y la
 * barra inferior visible solo dentro del flujo principal.
 *
 * Recibe el grafo de auth y la pestaña Ajustes como parámetros para que las pruebas puedan
 * sustituirlos sin Hilt.
 *
 * @param navController controlador que gobierna la navegación.
 * @param destinoInicial ruta de arranque: [AUTH_GRAPH_ROUTE] o [RUTA_GRAFO_PRINCIPAL].
 * @param grafoAuth añade el grafo de auth; recibe el callback que se invoca al autenticarse.
 * @param ajustes contenido de la pestaña Ajustes; recibe el callback que se invoca al cerrar sesión.
 */
@Composable
fun KronoShell(
    navController: NavHostController,
    destinoInicial: String,
    grafoAuth: NavGraphBuilder.(alAutenticarse: () -> Unit) -> Unit,
    ajustes: @Composable (alCerrarSesion: () -> Unit) -> Unit,
) {
    val entradaActual by navController.currentBackStackEntryAsState()
    val seleccionado = entradaActual?.destination.pestanaSeleccionada()

    Scaffold(
        containerColor = DeepMidnight,
        bottomBar = {
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
            startDestination = destinoInicial,
            modifier = Modifier.padding(padding),
        ) {
            grafoAuth { navController.irAlFlujoPrincipal() }
            grafoPrincipal(ajustes = { ajustes { navController.volverAAutenticacion() } })
        }
    }
}

/** Añade el flujo principal: una pantalla por pestaña de la barra inferior. */
private fun NavGraphBuilder.grafoPrincipal(ajustes: @Composable () -> Unit) {
    navigation(startDestination = KronoDestination.Inicial.route, route = RUTA_GRAFO_PRINCIPAL) {
        // Cada pantalla provisional se reemplaza por el grafo que exponga su feature
        // (dashboard, tasks, calendar, profile) cuando se implemente.
        composable(KronoDestination.Inicio.route) { SeccionProvisional(R.string.seccion_inicio) }
        composable(KronoDestination.Tareas.route) { SeccionProvisional(R.string.seccion_tareas) }
        composable(KronoDestination.Calendario.route) { SeccionProvisional(R.string.seccion_calendario) }
        composable(KronoDestination.Ajustes.route) { ajustes() }
    }
}

/** Pestaña de la barra que contiene al destino actual, o `null` fuera del flujo principal. */
private fun NavDestination?.pestanaSeleccionada(): KronoDestination? =
    KronoDestination.entries.firstOrNull { destino ->
        this?.hierarchy?.any { it.route == destino.route } == true
    }

/** Cambia de pestaña conservando y restaurando el estado de la anterior (F-01 CA-2). */
fun NavController.navegarAPestana(destino: KronoDestination) {
    navigate(destino.route) {
        popUpTo(KronoDestination.Inicial.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Entra al flujo principal tras autenticarse; "atrás" ya no vuelve al login. */
fun NavController.irAlFlujoPrincipal() {
    navigate(RUTA_GRAFO_PRINCIPAL) {
        popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
    }
}

/** Vuelve al grafo de auth tras cerrar sesión; "atrás" ya no regresa a la app. */
fun NavController.volverAAutenticacion() {
    navigate(AUTH_GRAPH_ROUTE) {
        popUpTo(RUTA_GRAFO_PRINCIPAL) { inclusive = true }
    }
}

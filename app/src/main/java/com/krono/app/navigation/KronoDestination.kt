package com.krono.app.navigation

import androidx.annotation.StringRes
import com.krono.app.R

/**
 * Destinos de la barra inferior. Solo `app` conoce estas rutas: ningún feature-*
 * las referencia ni importa a otro feature (CLAUDE.md).
 */
enum class KronoDestination(val route: String, @StringRes val labelRes: Int) {
    Inicio("inicio", R.string.nav_inicio),
    Tareas("tareas", R.string.nav_tareas),
    Calendario("calendario", R.string.nav_calendario),
    Ajustes("ajustes", R.string.nav_ajustes),
    ;

    companion object {
        val Inicial: KronoDestination = Inicio

        fun desdeRuta(route: String?): KronoDestination? = entries.firstOrNull { it.route == route }
    }
}

/** Ruta del grafo del flujo principal (con barra inferior). Un grafo de auth irá antes, como hermano. */
const val RUTA_GRAFO_PRINCIPAL = "principal"

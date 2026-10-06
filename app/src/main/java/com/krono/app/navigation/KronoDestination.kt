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

/** Ruta del grafo del flujo principal (con barra inferior). */
const val RUTA_GRAFO_PRINCIPAL = "principal"

/**
 * Destino tras iniciar sesión o crear cuenta mientras F-01 no se conecte al flujo
 * autenticado (F-22 a F-25 CA-5). Para activarlo, navegar a [RUTA_GRAFO_PRINCIPAL].
 */
const val RUTA_PROXIMO_HITO = "proximo_hito"

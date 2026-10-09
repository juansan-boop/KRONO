package com.krono.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Tamaños de componentes del Lumina Glass System (grid de 8dp). Complementa a
 * [KronoSpacing]: allí van márgenes y separaciones; aquí, dimensiones de elementos.
 */
object KronoSizes {
    /** Área táctil mínima de accesibilidad. */
    val minTouchTarget = 48.dp

    /** Alto de botones primarios y campos de formulario. */
    val controlHeight = 56.dp

    /** Ícono pequeño (acompaña texto en línea). */
    val iconSmall = 16.dp
    /** Ícono de tamaño estándar. */
    val iconMedium = 24.dp
    /** Ícono grande, para énfasis. */
    val iconLarge = 40.dp

    /** Logo de la marca en pantallas de acceso. */
    val brandLogo = 80.dp

    /** Símbolo de KRONO compacto: barras superiores, dentro del logo y en vistas previas. */
    val brandSymbolSmall = 32.dp
    /** Símbolo de KRONO de tamaño estándar. */
    val brandSymbol = 40.dp
    /** Símbolo de KRONO grande (vistas previas). */
    val brandSymbolLarge = 48.dp

    /** Insignia circular de confirmación (pantallas de éxito). */
    val successBadge = 120.dp

    /** Ancho máximo de formularios para que no se estiren en pantallas anchas. */
    val formMaxWidth = 480.dp

    /** Grosor de los indicadores de progreso circulares dentro de botones. */
    val progressStroke = 2.dp
}

package com.krono.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Valores del efecto "glass" de [glassSurface]; centralizados para ajustarlo en un solo lugar. */
object KronoGlass {
    /** Radio de las esquinas por defecto (igual al de `KronoShapes.medium`). */
    val cornerRadius: Dp = 24.dp

    /** Grosor del borde luminoso. */
    val borderWidth: Dp = 1.dp

    /** Opacidad del borde blanco. */
    val borderAlpha: Float = 0.12f

    /** Borde por defecto: blanco translúcido que simula el canto del vidrio. */
    val borderColor: Color = Color.White.copy(alpha = borderAlpha)

    /** Opacidad del color de la parte superior del degradado. */
    val topLayerAlpha: Float = 0.6f

    /** Opacidad del color de la parte inferior del degradado. */
    val bottomLayerAlpha: Float = 0.4f
}

/**
 * Modificador central del efecto "glass": cualquier superficie con aspecto de vidrio de la
 * app debe usarlo en vez de recrear el degradado y el borde a mano, de modo que el efecto
 * se ajuste globalmente desde [KronoGlass].
 *
 * Limitación conocida: no desenfoca lo que hay detrás de la superficie (backdrop blur), solo
 * simula el vidrio con degradado translúcido y borde. En Compose, `Modifier.blur` y
 * `RenderEffect` desenfocan el contenido del propio nodo, no el fondo; el desenfoque real
 * exige capturar el contenido de atrás (por ejemplo con una librería como Haze, que sería una
 * dependencia nueva) y un manejo distinto por versión de Android. Queda como deuda de diseño
 * a decidir con el equipo.
 *
 * @param cornerRadius radio de las esquinas de la superficie.
 * @param borderColor color del borde.
 */
fun Modifier.glassSurface(
    cornerRadius: Dp = KronoGlass.cornerRadius,
    borderColor: Color = KronoGlass.borderColor,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                SurfaceContainerHigh.copy(alpha = KronoGlass.topLayerAlpha),
                SurfaceContainer.copy(alpha = KronoGlass.bottomLayerAlpha),
            ),
        ),
    )
    .border(
        width = KronoGlass.borderWidth,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius),
    )

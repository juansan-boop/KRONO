package com.krono.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Todo múltiplo de 8dp, consistente con el grid del Lumina Glass System. */
val KronoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp),
)

/** Espaciados base del grid de 8dp — usar siempre estos tokens, nunca dp sueltos. */
object KronoSpacing {
    /** Separación mínima. */
    val xs = 4.dp
    /** Separación pequeña. */
    val sm = 8.dp
    /** Separación estándar. */
    val md = 16.dp
    /** Separación grande. */
    val lg = 24.dp
    /** Separación muy grande. */
    val xl = 32.dp
    /** Separación máxima. */
    val xxl = 48.dp
}

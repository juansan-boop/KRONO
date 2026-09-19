package com.krono.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Modificador central del efecto "glass" — cualquier superficie con aspecto de
 * vidrio esmerilado en la app debe usar este modificador en vez de recrear el
 * degradado/borde a mano. Mantiene consistencia visual entre feature-* y
 * facilita ajustar el efecto globalmente si el diseño cambia.
 *
 * Nota: el verdadero blur de fondo (backdrop blur) requiere Android 12+
 * (RenderEffect) o una librería de blur para versiones anteriores; este
 * modificador cubre el look de superficie (borde + degradado) — conectar el
 * blur real es un TODO de implementación.
 */
fun Modifier.glassSurface(
    cornerRadius: androidx.compose.ui.unit.Dp = 24.dp,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                SurfaceContainerHigh.copy(alpha = 0.6f),
                SurfaceContainer.copy(alpha = 0.4f),
            ),
        ),
    )
    .border(
        width = 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius),
    )

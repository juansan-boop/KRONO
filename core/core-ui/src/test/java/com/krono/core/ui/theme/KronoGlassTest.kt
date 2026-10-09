package com.krono.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Fija los valores visuales del efecto glass: cambiarlos es una decisión de diseño, no un accidente. */
class KronoGlassTest {

    @Test
    fun lasMedidasDelBordeYLaEsquinaConservanSusValores() {
        assertEquals(24.dp, KronoGlass.cornerRadius)
        assertEquals(1.dp, KronoGlass.borderWidth)
    }

    @Test
    fun lasTransparenciasConservanSusValores() {
        assertEquals(0.6f, KronoGlass.topLayerAlpha, 0f)
        assertEquals(0.4f, KronoGlass.bottomLayerAlpha, 0f)
        assertEquals(Color.White.copy(alpha = 0.12f), KronoGlass.borderColor)
    }
}

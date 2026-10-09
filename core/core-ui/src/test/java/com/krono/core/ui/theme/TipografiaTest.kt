package com.krono.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifica que la tipografía use las fuentes reales del sistema de diseño (F-27) y que los
 * tamaños salgan de tokens con nombre en lugar de literales sueltos.
 */
class TipografiaTest {

    /** Todos los estilos de la escala de Material 3, con su nombre para los mensajes de error. */
    private val estilos: Map<String, TextStyle>
        get() = mapOf(
            "displayLarge" to KronoTypography.displayLarge,
            "displayMedium" to KronoTypography.displayMedium,
            "displaySmall" to KronoTypography.displaySmall,
            "headlineLarge" to KronoTypography.headlineLarge,
            "headlineMedium" to KronoTypography.headlineMedium,
            "headlineSmall" to KronoTypography.headlineSmall,
            "titleLarge" to KronoTypography.titleLarge,
            "titleMedium" to KronoTypography.titleMedium,
            "titleSmall" to KronoTypography.titleSmall,
            "bodyLarge" to KronoTypography.bodyLarge,
            "bodyMedium" to KronoTypography.bodyMedium,
            "bodySmall" to KronoTypography.bodySmall,
            "labelLarge" to KronoTypography.labelLarge,
            "labelMedium" to KronoTypography.labelMedium,
            "labelSmall" to KronoTypography.labelSmall,
        )

    @Test
    fun ningunEstiloUsaLaFuenteDelSistema() {
        estilos.forEach { (nombre, estilo) ->
            assertNotNull("$nombre no declara fontFamily", estilo.fontFamily)
            assertNotEquals("$nombre usa FontFamily.Default", FontFamily.Default, estilo.fontFamily)
        }
    }

    @Test
    fun soloLabelLargeUsaJetBrainsMono_yElRestoUsaHankenGrotesk() {
        estilos.forEach { (nombre, estilo) ->
            val esperada = if (nombre == "labelLarge") JetBrainsMono else HankenGrotesk
            assertEquals("Familia de $nombre", esperada, estilo.fontFamily)
        }
    }

    @Test
    fun hankenGrotesk_noEsLaFuenteDelSistema() {
        assertNotEquals(FontFamily.Default, HankenGrotesk)
        assertNotEquals(FontFamily.Monospace, JetBrainsMono)
    }

    @Test
    fun escalaTipografica_usaHankenParaTextoYJetBrainsParaDatos() {
        assertEquals(HankenGrotesk, KronoTypography.bodyLarge.fontFamily)
        assertEquals(HankenGrotesk, KronoTypography.headlineLarge.fontFamily)
        assertEquals(JetBrainsMono, KronoTypography.labelLarge.fontFamily)
    }

    @Test
    fun escalaTipografica_tomaLosTamanosDeLosTokens() {
        assertEquals(KronoTextSize.Headline, KronoTypography.headlineLarge.fontSize)
        assertEquals(KronoTextSize.Title, KronoTypography.titleLarge.fontSize)
        assertEquals(KronoTextSize.Large, KronoTypography.bodyLarge.fontSize)
        assertEquals(KronoTextSize.Large, KronoTypography.titleMedium.fontSize)
        assertEquals(KronoTextSize.Medium, KronoTypography.bodyMedium.fontSize)
        assertEquals(KronoTextSize.Medium, KronoTypography.labelLarge.fontSize)
        assertEquals(KronoTextSize.Small, KronoTypography.labelMedium.fontSize)
        assertEquals(KronoTextSize.Small, KronoTypography.bodySmall.fontSize)
    }

    @Test
    fun tokensDeTamano_conservanLosValoresOriginales() {
        assertEquals(28.sp, KronoTextSize.Headline)
        assertEquals(22.sp, KronoTextSize.Title)
        assertEquals(16.sp, KronoTextSize.Large)
        assertEquals(14.sp, KronoTextSize.Medium)
        assertEquals(12.sp, KronoTextSize.Small)
    }
}

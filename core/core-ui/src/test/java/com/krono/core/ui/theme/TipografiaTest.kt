package com.krono.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.unit.sp
import com.krono.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifica que la tipografía use las fuentes reales del sistema de diseño (F-27) y que los
 * tamaños salgan de tokens con nombre en lugar de literales sueltos.
 */
class TipografiaTest {

    private fun tipografias(familia: FontFamily): List<ResourceFont> =
        (familia as FontListFontFamily).fonts.map { it as ResourceFont }

    @Test
    fun hankenGrotesk_noEsLaFuenteDelSistema() {
        assertNotEquals(FontFamily.Default, HankenGrotesk)
        assertNotEquals(FontFamily.Monospace, JetBrainsMono)
    }

    @Test
    fun hankenGrotesk_declaraLosPesosQueUsaLaEscala() {
        val pesos = tipografias(HankenGrotesk).associate { it.weight to it.resId }

        assertEquals(R.font.hanken_grotesk_regular, pesos[FontWeight.Normal])
        assertEquals(R.font.hanken_grotesk_medium, pesos[FontWeight.Medium])
        assertEquals(R.font.hanken_grotesk_semibold, pesos[FontWeight.SemiBold])
        assertEquals(R.font.hanken_grotesk_bold, pesos[FontWeight.Bold])
    }

    @Test
    fun jetBrainsMono_declaraElPesoQueUsaLaEscala() {
        val pesos = tipografias(JetBrainsMono).associate { it.weight to it.resId }

        assertEquals(R.font.jetbrains_mono_medium, pesos[FontWeight.Medium])
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

    @Test
    fun archivosDeFuenteYLicenciasExistenConNombresValidosParaAndroid() {
        val esperados = listOf(
            "src/main/res/font/hanken_grotesk_regular.ttf",
            "src/main/res/font/hanken_grotesk_medium.ttf",
            "src/main/res/font/hanken_grotesk_semibold.ttf",
            "src/main/res/font/hanken_grotesk_bold.ttf",
            "src/main/res/font/jetbrains_mono_medium.ttf",
            "licenses/OFL-HankenGrotesk.txt",
            "licenses/OFL-JetBrainsMono.txt",
        )
        esperados.forEach { ruta ->
            val archivo = File(ruta)
            assertTrue("Falta $ruta", archivo.exists() && archivo.length() > 0)
        }
    }

    @Test
    fun fontsDeCompose_seSigueCreandoDesdeRecursos() {
        // Guardia: Font(resId) debe producir ResourceFont, no una fuente descargable.
        assertTrue(Font(R.font.hanken_grotesk_regular) is ResourceFont)
    }
}

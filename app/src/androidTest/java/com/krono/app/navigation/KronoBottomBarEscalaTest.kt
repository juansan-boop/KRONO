package com.krono.app.navigation

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.app.R
import com.krono.core.ui.theme.KronoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil

/** La barra inferior no debe partir las etiquetas con la fuente al 200 %, ni en pantallas compactas. */
class KronoBottomBarEscalaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun conFuenteAl200PorCientoCadaEtiquetaCabeEnUnaLineaDentroDeSuPestana() {
        composeRule.setContent {
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, ESCALA_MAXIMA)) {
                KronoTheme {
                    KronoBottomBar(
                        seleccionado = KronoDestination.Inicio,
                        onSeleccionar = {},
                        modifier = Modifier.width(ANCHO_PANTALLA_COMPACTA),
                    )
                }
            }
        }
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext

        KronoDestination.entries.forEach { destino ->
            val etiqueta = contexto.getString(destino.labelRes)
            val nodoEtiqueta = composeRule.onNodeWithText(etiqueta, useUnmergedTree = true)
            val resultados = mutableListOf<TextLayoutResult>()
            nodoEtiqueta.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(resultados)
            val limitesEtiqueta = nodoEtiqueta.fetchSemanticsNode().boundsInRoot
            val limitesPestana = composeRule
                .onNodeWithContentDescription(contexto.getString(R.string.nav_ir_a, etiqueta))
                .fetchSemanticsNode().boundsInRoot

            assertEquals("${destino.name} ocupa más de una línea", 1, resultados.first().lineCount)
            assertTrue(
                "${destino.name} se recorta: no cabe completa en su pestaña",
                ceil(resultados.first().multiParagraph.maxIntrinsicWidth) <= resultados.first().size.width,
            )
            assertTrue(
                "${destino.name} se sale de su pestaña",
                limitesEtiqueta.left >= limitesPestana.left && limitesEtiqueta.right <= limitesPestana.right,
            )
        }
    }

    private companion object {
        /** Escala máxima de fuente que ofrece Android en Ajustes de accesibilidad. */
        const val ESCALA_MAXIMA = 2f

        /** Ancho de los teléfonos compactos más habituales (el más estrecho que se soporta). */
        val ANCHO_PANTALLA_COMPACTA = 360.dp
    }
}

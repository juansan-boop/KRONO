package com.krono.app.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.app.R
import com.krono.core.ui.theme.KronoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil

/** Los botones de Ajustes y de su diálogo deben mostrar su texto completo con la fuente al 200 %. */
class PantallaAjustesEscalaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun mostrarAjustesConDialogoAbierto() {
        composeRule.setContent {
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, ESCALA_MAXIMA)) {
                KronoTheme {
                    PantallaAjustes(
                        confirmacionVisible = true,
                        cerrando = false,
                        hayError = false,
                        alSolicitarCierre = {},
                        alCancelarCierre = {},
                        alConfirmarCierre = {},
                    )
                }
            }
        }
    }

    private fun verificarTextosCompletos(textoRes: Int) {
        val texto = InstrumentationRegistry.getInstrumentation().targetContext.getString(textoRes)
        val nodos = composeRule.onAllNodesWithText(texto, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("No se encontró '$texto'", nodos.isNotEmpty())
        nodos.forEach { nodo ->
            val resultados = mutableListOf<TextLayoutResult>()
            nodo.config[SemanticsActions.GetTextLayoutResult].action?.invoke(resultados)
            val disposicion = resultados.first()
            assertTrue(
                "'$texto' se recorta: necesita ${disposicion.multiParagraph.height}px y tiene ${disposicion.size.height}px",
                ceil(disposicion.multiParagraph.height) <= disposicion.size.height,
            )
        }
    }

    @Test
    fun conFuenteAl200PorCientoLosBotonesCerrarSesionMuestranSuTextoCompleto() {
        mostrarAjustesConDialogoAbierto()

        verificarTextosCompletos(R.string.cerrar_sesion)
    }

    @Test
    fun conFuenteAl200PorCientoElBotonCancelarMuestraSuTextoCompleto() {
        mostrarAjustesConDialogoAbierto()

        verificarTextosCompletos(R.string.cancelar)
    }

    private companion object {
        /** Escala máxima de fuente que ofrece Android en Ajustes de accesibilidad. */
        const val ESCALA_MAXIMA = 2f
    }
}

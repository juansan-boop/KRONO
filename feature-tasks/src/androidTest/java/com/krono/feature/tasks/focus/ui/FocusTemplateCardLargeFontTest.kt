package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focus.FocusTemplatesUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Regresión de la tarjeta de plantilla con la fuente al 200 % en 360 dp (F-10, PR #46): con una
 * sola tarjeta (la activa, sin botón de eliminar) las etiquetas "Foco" y "Descanso" de la barra
 * desaparecían porque la altura intrínseca de la fila no incluía el `FlowRow`.
 */
class FocusTemplateCardLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    // La lista es perezosa y solo compone lo que cabe en la ventana: con la fuente al 200 % caben dos.
    private val visibleTemplates = DefaultFocusTemplates.all.take(VISIBLE_TEMPLATES)

    private val focusLabel get() = str(R.string.focus_percent_focus, DEEP_STUDY_FOCUS_PERCENT)
    private val breakLabel get() = str(R.string.focus_percent_break, DEEP_STUDY_BREAK_PERCENT)

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun showAtLargeFont(state: FocusTemplatesUiState, screenHeight: Dp = SCREEN_HEIGHT) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, LARGE_FONT_SCALE)) {
                KronoTheme {
                    FocusTemplatesScreen(
                        state = state,
                        actions = FocusTemplatesActions(),
                        modifier = Modifier.width(SCREEN_WIDTH).height(screenHeight),
                    )
                }
            }
        }
    }

    /** Exige que la etiqueta exista con altura real, dentro del ancho y sin partir palabras. */
    private fun assertLabelPresentAndWhole(label: String) {
        val node = composeRule.onNode(hasText(label), useUnmergedTree = true).fetchSemanticsNode()
        val screenWidthPx = with(composeRule.density) { SCREEN_WIDTH.toPx() }
        assertTrue("Etiqueta sin altura: $label", node.size.height > 0)
        assertTrue("Etiqueta fuera del ancho: $label", node.boundsInRoot.right <= screenWidthPx)
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        val result = results.first()
        val text = result.layoutInput.text.text
        (0 until result.lineCount - 1).forEach { line ->
            val end = result.getLineEnd(line)
            assertTrue("Palabra partida en \"$text\"", text[end - 1].isWhitespace() || text[end].isWhitespace())
        }
    }

    @Test
    fun unaSolaTarjetaActiva_alDoscientosPorCiento_muestraFocoYDescanso() {
        showAtLargeFont(FocusTemplatesUiState(isLoading = false, templates = listOf(DefaultFocusTemplates.deepStudy)))

        composeRule.onNode(hasText(focusLabel), useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNode(hasText(breakLabel), useUnmergedTree = true).assertIsDisplayed()
        assertLabelPresentAndWhole(focusLabel)
        assertLabelPresentAndWhole(breakLabel)
    }

    @Test
    fun unaSolaTarjetaActiva_alDoscientosPorCiento_noOfreceEliminar() {
        showAtLargeFont(FocusTemplatesUiState(isLoading = false, templates = listOf(DefaultFocusTemplates.deepStudy)))

        val deleteLabel = str(R.string.focus_cd_delete, DefaultFocusTemplates.deepStudy.name)
        assertEquals(0, composeRule.onAllNodes(hasContentDescription(deleteLabel)).fetchSemanticsNodes().size)
    }

    @Test
    fun variasTarjetasConActivar_alDoscientosPorCiento_muestranSusEtiquetas() {
        showAtLargeFont(
            FocusTemplatesUiState(isLoading = false, templates = visibleTemplates),
            screenHeight = TALL_SCREEN_HEIGHT,
        )

        visibleTemplates.forEach { template ->
            assertLabelPresentAndWhole(str(R.string.focus_percent_focus, template.focusPercent))
            assertLabelPresentAndWhole(str(R.string.focus_percent_break, template.breakPercent))
            if (!template.isActive) {
                composeRule.onNode(hasContentDescription(str(R.string.focus_cd_activate, template.name))).assertExists()
            }
        }
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val VISIBLE_TEMPLATES = 2
        const val DEEP_STUDY_FOCUS_PERCENT = 82
        const val DEEP_STUDY_BREAK_PERCENT = 18
        val SCREEN_WIDTH = 360.dp
        val SCREEN_HEIGHT = 640.dp

        // Alto suficiente para que la lista perezosa componga las tres tarjetas.
        val TALL_SCREEN_HEIGHT = 3000.dp
    }
}

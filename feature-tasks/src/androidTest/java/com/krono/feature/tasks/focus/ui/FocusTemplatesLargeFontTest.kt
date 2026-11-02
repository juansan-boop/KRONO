package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focus.FocusTemplateEditorState
import com.krono.feature.tasks.focus.FocusTemplatesUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de UI de [FocusTemplatesScreen] con la fuente al 200 % en un ancho de 360 dp (F-10,
 * criterio de accesibilidad): ninguna palabra se parte a mitad, ningún texto se trunca y nada se
 * sale del ancho. Cubre la lista, el formulario con errores y la confirmación de eliminar.
 */
class FocusTemplatesLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val templates = listOf(
        DefaultFocusTemplates.deepStudy,
        DefaultFocusTemplates.nightReading,
        DefaultFocusTemplates.quickSprint,
        FocusTemplate("long", "Planificación semanal de laboratorio", 90, 15, isActive = false),
    )
    private val loaded = FocusTemplatesUiState(isLoading = false, templates = templates)

    private fun showAtLargeFont(state: FocusTemplatesUiState) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, LARGE_FONT_SCALE)) {
                KronoTheme {
                    FocusTemplatesScreen(
                        state = state,
                        actions = FocusTemplatesActions(),
                        modifier = Modifier.width(SCREEN_WIDTH).height(TALL_SCREEN_HEIGHT),
                    )
                }
            }
        }
    }

    private fun str(id: Int): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun textNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.layoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    /** `true` si cada salto de línea cae en un espacio o al final del texto, nunca dentro de una palabra. */
    private fun TextLayoutResult.breaksOnlyBetweenWords(): Boolean {
        val text = layoutInput.text.text
        return (0 until lineCount - 1).all { line ->
            val end = getLineEnd(line)
            text[end - 1].isWhitespace() || text[end].isWhitespace()
        }
    }

    private fun TextLayoutResult.hasEllipsizedLine(): Boolean = (0 until lineCount).any { isLineEllipsized(it) }

    private fun assertNoBrokenWordsNorTruncation(minTexts: Int) {
        val nodes = textNodes()
        assertTrue("Debe haber textos que revisar, hay ${nodes.size}", nodes.size >= minTexts)
        nodes.forEach { node ->
            val result = node.layoutResult()
            val text = result.layoutInput.text.text
            assertTrue("Palabra partida en \"$text\"", result.breaksOnlyBetweenWords())
            assertFalse("Texto truncado en \"$text\"", result.hasEllipsizedLine())
        }
    }

    @Test
    fun lista_alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        showAtLargeFont(loaded)

        assertNoBrokenWordsNorTruncation(minTexts = MIN_LIST_TEXTS)
    }

    @Test
    fun lista_alDoscientosPorCiento_losTextosQuedanDentroDelAncho() {
        showAtLargeFont(loaded)

        val screenWidthPx = with(composeRule.density) { SCREEN_WIDTH.toPx() }
        textNodes().forEach { node ->
            assertTrue(
                "Texto fuera del ancho: ${node.layoutResult().layoutInput.text.text}",
                node.boundsInRoot.right <= screenWidthPx,
            )
        }
    }

    @Test
    fun lista_alDoscientosPorCiento_elBotonCrearConservaSuTextoCompleto() {
        showAtLargeFont(loaded)

        val label = str(R.string.focus_create)
        val result = textNodes().map { it.layoutResult() }.first { it.layoutInput.text.text == label }
        assertFalse(result.hasEllipsizedLine())
        assertTrue(result.breaksOnlyBetweenWords())
    }

    @Test
    fun formularioConErrores_alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        val editor = FocusTemplateEditorState(
            name = "",
            focusMinutesText = "0",
            breakMinutesText = "",
            nameErrorRes = R.string.focus_error_name_blank,
            focusErrorRes = R.string.focus_error_focus_minutes,
            breakErrorRes = R.string.focus_error_break_minutes,
        )
        showAtLargeFont(loaded.copy(editor = editor))

        assertNoBrokenWordsNorTruncation(minTexts = MIN_FORM_TEXTS)
    }

    @Test
    fun confirmacionDeEliminar_alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        showAtLargeFont(loaded.copy(templatePendingDelete = templates.first()))

        assertNoBrokenWordsNorTruncation(minTexts = MIN_DIALOG_TEXTS)
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val MIN_LIST_TEXTS = 10
        const val MIN_FORM_TEXTS = 8
        const val MIN_DIALOG_TEXTS = 6
        val SCREEN_WIDTH = 360.dp
        val TALL_SCREEN_HEIGHT = 3000.dp
    }
}

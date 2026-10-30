package com.krono.feature.dashboard.ui

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
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.dashboard.DashboardUiState
import com.krono.feature.dashboard.PriorityTaskUi
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas de UI de [DashboardScreen] con la fuente al 200 % en un ancho de 360 dp (F-13, criterio
 * de accesibilidad): ninguna palabra se parte a mitad, ningún texto se trunca y nada se sale del
 * contenedor.
 */
class DashboardLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tasks = listOf(
        PriorityTaskUi("t1", "Informe final", LocalTime(10, 30), TaskCategory.TRABAJO, TaskPriority.ALTA),
        PriorityTaskUi("t2", "Leer capitulo", LocalTime(16, 0), TaskCategory.ESTUDIO, TaskPriority.MEDIA),
        PriorityTaskUi("t3", "Trabajo", null, TaskCategory.PERSONAL, TaskPriority.BAJA),
    )

    private val state = DashboardUiState.Content(
        userName = "Alexa",
        completedPercent = 75,
        completedCount = 6,
        pendingCount = 2,
        focusTime = 90.minutes,
        hasTasksToday = true,
        priorityTasks = tasks,
    )

    private fun showAtLargeFont() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, LARGE_FONT_SCALE)) {
                KronoTheme {
                    DashboardScreen(
                        state = state,
                        actions = DashboardActions(),
                        modifier = Modifier.width(SCREEN_WIDTH).height(TALL_SCREEN_HEIGHT),
                    )
                }
            }
        }
    }

    private fun textNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.layoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    /** `true` si cada salto de línea cae en un espacio o al final del texto, nunca dentro de una palabra. */
    private fun TextLayoutResult.breaksOnlyBetweenWords(): Boolean {
        val text = layoutInput.text.text
        return (0 until lineCount - 1).all { line ->
            val end = getLineEnd(line)
            text[end - 1].isWhitespace() || text[end].isWhitespace()
        }
    }

    /** `true` si alguna línea terminó en puntos suspensivos, es decir, se perdió texto. */
    private fun TextLayoutResult.hasEllipsizedLine(): Boolean =
        (0 until lineCount).any { isLineEllipsized(it) }

    @Test
    fun alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        showAtLargeFont()

        val nodes = textNodes()
        assertTrue("Debe haber textos que revisar", nodes.size > MIN_EXPECTED_TEXTS)
        nodes.forEach { node ->
            val result = node.layoutResult()
            val text = result.layoutInput.text.text
            assertTrue("Palabra partida en \"$text\"", result.breaksOnlyBetweenWords())
            assertFalse("Texto truncado en \"$text\"", result.hasEllipsizedLine())
        }
    }

    @Test
    fun alDoscientosPorCiento_losTextosQuedanDentroDelAncho() {
        showAtLargeFont()

        val screenWidthPx = with(composeRule.density) { SCREEN_WIDTH.toPx() }
        textNodes().forEach { node ->
            val right = node.boundsInRoot.right
            assertTrue(
                "Texto fuera del ancho en ${node.layoutResult().layoutInput.text.text}",
                right <= screenWidthPx,
            )
        }
    }

    @Test
    fun alDoscientosPorCiento_lasEtiquetasDelResumenNoSeRompenYConservanSuTexto() {
        showAtLargeFont()

        val labels = textNodes().map { it.layoutResult() }.filter {
            it.layoutInput.text.text in setOf("Completadas", "Pendientes")
        }
        assertEquals(2, labels.size)
        labels.forEach { assertEquals(1, it.lineCount) }
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val MIN_EXPECTED_TEXTS = 10
        val SCREEN_WIDTH = 360.dp
        val TALL_SCREEN_HEIGHT = 3000.dp
    }
}

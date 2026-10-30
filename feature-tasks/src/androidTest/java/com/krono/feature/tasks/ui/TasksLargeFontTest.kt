package com.krono.feature.tasks.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.TasksUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas de UI de la lista de tareas con fuente grande en 360 dp (criterio de accesibilidad):
 * ninguna palabra se parte, no desaparece la etiqueta de estimación, "Tus Tareas" y "Enfoque
 * Diario" se leen completos y los controles miden al menos 48 dp.
 */
class TasksLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate(2026, 10, 30)
    private val task = Task(
        id = "t1",
        title = "Repasar apuntes de Cálculo",
        category = TaskCategory.ESTUDIO,
        priority = TaskPriority.MEDIA,
        dueDate = LocalDateTime(2026, 10, 30, 9, 30),
        estimatedDuration = 45.minutes,
    )
    private val state = TasksUiState(
        isLoading = false,
        today = today,
        dailyFocusPercentage = 33,
        dailyPendingCount = 4,
        todayTasks = listOf(task),
    )

    private fun show(fontScale: Float) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KronoTheme {
                    TasksScreen(
                        state = state,
                        onFilterSelected = {},
                        onToggleCompletion = {},
                        onDeleteTask = {},
                        onAddTask = {},
                        onOpenAssistant = {},
                        modifier = Modifier.width(SCREEN_WIDTH).height(TALL_SCREEN_HEIGHT),
                    )
                }
            }
        }
    }

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun textNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()

    private fun SemanticsNode.layoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    private fun TextLayoutResult.breaksOnlyBetweenWords(): Boolean {
        val text = layoutInput.text.text
        return (0 until lineCount - 1).all { line ->
            val end = getLineEnd(line)
            text[end - 1].isWhitespace() || text[end].isWhitespace()
        }
    }

    private fun assertTextShownWhole(text: String) {
        val result = textNodes().map { it.layoutResult() }.firstOrNull { it.layoutInput.text.text == text }
            ?: throw AssertionError("No existe el texto \"$text\"; hay: ${textNodes().map { it.layoutResult().layoutInput.text.text }}")
        assertTrue("Palabra partida en \"$text\" (${result.size}, líneas ${result.lineCount})", result.breaksOnlyBetweenWords())
        assertFalse("Texto truncado en \"$text\"", (0 until result.lineCount).any { result.isLineEllipsized(it) })
    }

    private fun assertScreenKeepsEverythingReadable(fontScale: Float) {
        show(fontScale)

        assertTextShownWhole(str(R.string.tasks_title))
        assertTextShownWhole(str(R.string.daily_focus_title))
        assertTextShownWhole(str(R.string.estimated_duration_format, str(R.string.duration_minutes, 45)))
        assertTextShownWhole(task.title)
        textNodes().forEach { node ->
            val result = node.layoutResult()
            assertTrue("Palabra partida en \"${result.layoutInput.text.text}\"", result.breaksOnlyBetweenWords())
        }
        val minPx = with(composeRule.density) { KronoSizes.minTouchTarget.toPx() }
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue("Control menor de 48 dp: ${node.config}", node.size.height >= minPx - 1f && node.size.width >= minPx - 1f)
        }
    }

    @Test
    fun alDoscientosPorCiento_nadaSePartePorDentroNiDesapareceLaEstimacion() = assertScreenKeepsEverythingReadable(2f)

    @Test
    fun alCientoTreintaPorCiento_nadaSePartePorDentroNiDesapareceLaEstimacion() = assertScreenKeepsEverythingReadable(1.3f)

    @Test
    fun alDoscientosPorCiento_laEstimacionOcupaAlmenosSuTexto() {
        show(2f)

        val label = str(R.string.estimated_duration_format, str(R.string.duration_minutes, 45))
        val lines = textNodes().map { it.layoutResult() }.first { it.layoutInput.text.text == label }.lineCount
        assertEquals("La etiqueta de estimación debe caber sin apilarse en muchas líneas", true, lines <= MAX_BADGE_LINES)
    }

    private companion object {
        val SCREEN_WIDTH = 360.dp
        val TALL_SCREEN_HEIGHT = 3000.dp
        const val MAX_BADGE_LINES = 2
    }
}

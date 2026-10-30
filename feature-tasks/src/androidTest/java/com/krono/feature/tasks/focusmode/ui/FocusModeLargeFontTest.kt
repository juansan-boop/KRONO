package com.krono.feature.tasks.focusmode.ui

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
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.FocusPhase
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focusmode.FocusModeUiState
import com.krono.feature.tasks.timer.ui.ClockRing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de UI de [FocusModeScreen] con la fuente al 200 % en un ancho de 360 dp (criterio de
 * accesibilidad): ningún texto clave desaparece, se trunca ni se parte a mitad de palabra, y nada
 * se sale del ancho. Cubre la pantalla en trabajo, en descanso y con el aviso de bloque completo.
 */
class FocusModeLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val task = Task(
        id = "t",
        title = "Entrega del informe trimestral de laboratorio",
        subtasks = listOf(
            Subtask("s1", "t", "Reunir datos", isDone = true, position = 0),
            Subtask("s2", "t", "Armar tablas comparativas", isDone = false, position = 1),
            Subtask("s3", "t", "Redactar conclusiones ejecutivas", isDone = false, position = 2),
        ),
    )
    private val working = FocusModeUiState(
        isLoading = false,
        task = task,
        phase = FocusPhase.WORKING,
        remaining = 60.minutes,
        blockDuration = 60.minutes,
        templateBreak = 15.minutes,
    )

    private fun showAtLargeFont(state: FocusModeUiState) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, LARGE_FONT_SCALE)) {
                KronoTheme {
                    FocusModeScreen(
                        state = state,
                        actions = FocusModeActions(),
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

    private fun TextLayoutResult.hasEllipsizedLine(): Boolean = (0 until lineCount).any { isLineEllipsized(it) }

    private fun textResult(text: String): TextLayoutResult =
        textNodes().map { it.layoutResult() }.firstOrNull { it.layoutInput.text.text == text }
            ?: throw AssertionError("No existe el texto \"$text\"; hay: ${textNodes().map { it.layoutResult().layoutInput.text.text }}")

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
    fun enTrabajo_alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        showAtLargeFont(working)

        assertNoBrokenWordsNorTruncation(minTexts = MIN_SCREEN_TEXTS)
    }

    @Test
    fun enTrabajo_alDoscientosPorCiento_losTextosClaveSiguenPresentes() {
        showAtLargeFont(working)

        listOf(
            task.title,
            str(R.string.focus_mode_step, 2, 3, "Armar tablas comparativas"),
            str(R.string.focus_mode_extra_break, 5),
            str(R.string.focus_mode_finish),
            str(R.string.focus_mode_caption_remaining),
            str(R.string.focus_mode_chip_working),
        ).forEach { textResult(it) }
    }

    @Test
    fun enTrabajo_alDoscientosPorCiento_elRelojCabeEnUnaLineaDentroDelAnillo() {
        showAtLargeFont(working)

        val clock = textResult("60:00")
        assertEquals(1, clock.lineCount)
        assertFalse("El reloj desborda el anillo", clock.didOverflowWidth)
    }

    @Test
    fun enTrabajo_alDoscientosPorCiento_losTextosQuedanDentroDelAncho() {
        showAtLargeFont(working)

        val screenWidthPx = with(composeRule.density) { SCREEN_WIDTH.toPx() }
        textNodes().forEach { node ->
            assertTrue(
                "Texto fuera del ancho: ${node.layoutResult().layoutInput.text.text}",
                node.boundsInRoot.right <= screenWidthPx,
            )
        }
    }

    @Test
    fun enDescansoYConLaTareaEliminada_alDoscientosPorCiento_ningunaPalabraSeParteNiSeTrunca() {
        showAtLargeFont(
            working.copy(
                phase = FocusPhase.ON_BREAK,
                breakRemaining = 14.minutes + 30.seconds,
                breakDuration = 15.minutes,
                isTaskDeleted = true,
            ),
        )

        assertNoBrokenWordsNorTruncation(minTexts = MIN_SCREEN_TEXTS)
        textResult(str(R.string.focus_mode_task_deleted))
        textResult(str(R.string.focus_mode_caption_break))
    }

    @Test
    fun avisoDeBloqueCompleto_alDoscientosPorCiento_conservaTitulosYBotones() {
        showAtLargeFont(working.copy(phase = FocusPhase.BLOCK_COMPLETE, remaining = 0.seconds))

        assertNoBrokenWordsNorTruncation(minTexts = MIN_DIALOG_TEXTS)
        textResult(str(R.string.focus_mode_complete_title))
        textResult(str(R.string.focus_mode_complete_break, 15))
        textResult(str(R.string.focus_mode_complete_finish))
    }

    @Test
    fun sesionMuyCorta_alDoscientosPorCiento_elAvisoNoPartePalabras() {
        showAtLargeFont(working.copy(phase = null, blockDuration = 0.seconds, isSessionTooShort = true))

        textResult(str(R.string.timer_too_short))
        assertNoBrokenWordsNorTruncation(minTexts = 2)
    }

    /**
     * Android 13 y anteriores (el proyecto admite desde la API 26) escalan la fuente de forma lineal,
     * sin el amortiguado de las versiones nuevas: el reloj crece el doble y debe seguir cabiendo.
     */
    @Test
    fun relojEnElAnillo_conEscalaLinealDeFuente_cabeEnUnaLinea() {
        composeRule.setContent {
            val base = LocalDensity.current
            val linear = object : Density {
                override val density = base.density
                override val fontScale = LARGE_FONT_SCALE
            }
            CompositionLocalProvider(LocalDensity provides linear) {
                KronoTheme { ClockRing(clock = "180:00", progress = 1f, caption = "Tiempo restante", description = "reloj") }
            }
        }

        val clock = textResult("180:00")
        assertEquals(1, clock.lineCount)
        assertFalse("El reloj desborda el anillo", clock.didOverflowWidth)
    }

    private companion object {
        const val LARGE_FONT_SCALE = 2f
        const val MIN_SCREEN_TEXTS = 8
        const val MIN_DIALOG_TEXTS = 4
        val SCREEN_WIDTH = 360.dp
        val TALL_SCREEN_HEIGHT = 3000.dp
    }
}

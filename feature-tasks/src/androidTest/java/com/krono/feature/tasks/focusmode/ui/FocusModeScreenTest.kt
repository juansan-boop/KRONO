package com.krono.feature.tasks.focusmode.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.task.Subtask
import com.krono.core.domain.task.Task
import com.krono.core.domain.timer.FocusPhase
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focusmode.FocusModeUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de UI de [FocusModeScreen] (F-09 CA-1 a CA-3): cuenta regresiva, tarea y paso actual,
 * controles de pausa, descanso y finalizar, y el aviso al llegar a cero.
 */
class FocusModeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val steps = listOf(
        Subtask("s1", "t", "Reunir datos", isDone = true, position = 0),
        Subtask("s2", "t", "Armar tablas", isDone = true, position = 1),
        Subtask("s3", "t", "Redactar conclusiones ejecutivas", isDone = false, position = 2),
        Subtask("s4", "t", "Revisar", isDone = false, position = 3),
    )
    private val working = FocusModeUiState(
        isLoading = false,
        task = Task(id = "t", title = "Entrega Informe Trimestral", subtasks = steps),
        phase = FocusPhase.WORKING,
        remaining = 31.minutes + 49.seconds,
        blockDuration = 45.minutes,
        templateBreak = 10.minutes,
    )

    private val calls = mutableListOf<String>()
    private val actions = FocusModeActions(
        onPause = { calls += "pause" },
        onResume = { calls += "resume" },
        onExtraBreak = { calls += "extraBreak" },
        onTakeTemplateBreak = { calls += "templateBreak" },
        onFinish = { calls += "finish" },
        onConfirmSwitch = { calls += "confirmSwitch" },
        onDismissSwitch = { calls += "dismissSwitch" },
    )

    private fun show(state: FocusModeUiState) {
        composeRule.setContent { KronoTheme { FocusModeScreen(state = state, actions = actions) } }
    }

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @Test
    fun muestraLaCuentaRegresivaLaTareaYElPasoActual() {
        show(working)

        composeRule.onNodeWithContentDescription(str(R.string.cd_focus_mode_remaining, "31:49")).assertExists()
        composeRule.onNodeWithText("Entrega Informe Trimestral").assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_step, 3, 4, "Redactar conclusiones ejecutivas")).assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_block_minutes, 45)).assertExists()
    }

    @Test
    fun unaTareaSinSubtareas_noMuestraPaso() {
        show(working.copy(task = Task(id = "t", title = "Sin pasos")))

        composeRule.onNodeWithText("Sin pasos").assertExists()
        composeRule.onNodeWithText("Paso", substring = true).assertDoesNotExist()
    }

    @Test
    fun pausar_avisaAlViewModel() {
        show(working)

        composeRule.onNodeWithContentDescription(str(R.string.cd_focus_mode_pause)).performClick()

        assertEquals(listOf("pause"), calls)
    }

    @Test
    fun enPausa_elBotonPrincipalReanuda() {
        show(working.copy(phase = FocusPhase.PAUSED))

        composeRule.onNodeWithContentDescription(str(R.string.cd_focus_mode_resume)).performClick()

        assertEquals(listOf("resume"), calls)
    }

    @Test
    fun masCincoMinutosDeDescanso_avisaAlViewModel() {
        show(working)

        composeRule.onNodeWithText("+5m Descanso").performClick()

        assertEquals(listOf("extraBreak"), calls)
    }

    @Test
    fun finalizar_avisaAlViewModel() {
        show(working)

        composeRule.onNodeWithText(str(R.string.focus_mode_finish)).performClick()

        assertEquals(listOf("finish"), calls)
    }

    @Test
    fun enDescanso_muestraLaCuentaDelDescansoYNoPermiteOtroDescanso() {
        show(working.copy(phase = FocusPhase.ON_BREAK, breakRemaining = 4.minutes + 5.seconds, breakDuration = 5.minutes))

        composeRule.onNodeWithContentDescription(str(R.string.cd_focus_mode_break_remaining, "04:05")).assertExists()
        composeRule.onNodeWithText("+5m Descanso").assertIsNotEnabled()
        composeRule.onNodeWithText(str(R.string.focus_mode_finish)).assertIsEnabled()
    }

    @Test
    fun alTerminarElDescanso_avisaYElBotonPrincipalReanuda() {
        show(working.copy(phase = FocusPhase.BREAK_OVER))

        composeRule.onNodeWithText(str(R.string.focus_mode_break_over)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.cd_focus_mode_resume)).assertExists()
    }

    @Test
    fun alLlegarACero_avisaYOfreceElDescansoDeLaPlantilla() {
        show(working.copy(phase = FocusPhase.BLOCK_COMPLETE, remaining = 0.seconds))

        composeRule.onNodeWithText(str(R.string.focus_mode_complete_title)).assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_complete_break, 10)).performClick()

        assertEquals(listOf("templateBreak"), calls)
    }

    @Test
    fun alLlegarACero_sePuedeFinalizarDesdeElAviso() {
        show(working.copy(phase = FocusPhase.BLOCK_COMPLETE, remaining = 0.seconds))

        composeRule.onNode(hasText(str(R.string.focus_mode_complete_finish)) and hasClickAction() and hasAnyAncestor(isDialog())).performClick()

        assertEquals(listOf("finish"), calls)
    }

    @Test
    fun conOtraSesionActiva_pideConfirmarElCambio() {
        show(working.copy(phase = null, isConfirmingSwitch = true))

        composeRule.onNodeWithText(str(R.string.focus_mode_switch_title)).assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_switch_confirm)).performClick()

        assertEquals(listOf("confirmSwitch"), calls)
    }

    @Test
    fun conLaTareaEliminada_avisaQueElTiempoNoSeSumara() {
        show(working.copy(isTaskDeleted = true))

        composeRule.onNodeWithText(str(R.string.focus_mode_task_deleted)).assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_finish)).assertIsEnabled()
    }

    @Test
    fun alFinalizar_confirmaLaSesionGuardada() {
        show(working.copy(phase = null, savedSession = 20.minutes))

        composeRule.onNodeWithText(str(R.string.timer_saved, "20:00")).assertExists()
    }

    @Test
    fun sinEnfoqueEnCurso_avisaEnVezDeMostrarControles() {
        show(working.copy(phase = null))

        composeRule.onNodeWithText(str(R.string.focus_mode_idle)).assertExists()
        composeRule.onNodeWithText(str(R.string.focus_mode_finish)).assertDoesNotExist()
    }
}

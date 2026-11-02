package com.krono.feature.tasks.focus.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focus.FocusTemplateEditorState
import com.krono.feature.tasks.focus.FocusTemplatesUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de UI de [FocusTemplatesScreen] (F-10): lista con porcentajes (CA-3), acciones de
 * activar, editar y eliminar (CA-2), formulario con validación, estados de carga, vacío y error, y
 * áreas táctiles de 48 dp.
 */
class FocusTemplatesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val templates = listOf(
        DefaultFocusTemplates.deepStudy,
        DefaultFocusTemplates.nightReading,
        DefaultFocusTemplates.quickSprint,
    )
    private val loaded = FocusTemplatesUiState(isLoading = false, templates = templates)

    private fun str(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun show(state: FocusTemplatesUiState, actions: FocusTemplatesActions = FocusTemplatesActions()) {
        composeRule.setContent { KronoTheme { FocusTemplatesScreen(state = state, actions = actions) } }
    }

    @Test
    fun muestraLasTresPlantillasConLaActivaMarcada() {
        show(loaded)

        composeRule.onNodeWithText("Estudio Profundo").assertIsDisplayed()
        composeRule.onNodeWithText("Sprint Rápido").assertIsDisplayed()
        composeRule.onNodeWithText("Lectura Nocturna").assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.focus_active_badge)).assertCountEquals(1)
    }

    @Test
    fun cadaPlantillaIndicaElPorcentajeDeFocoYDescanso() {
        show(loaded)

        composeRule.onNodeWithText(str(R.string.focus_percent_focus, 82)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_percent_break, 18)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_percent_focus, 83)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_percent_focus, 80)).assertIsDisplayed()
    }

    @Test
    fun soloLasInactivasOfrecenActivar_yAlTocarlaSeNotificaSuId() {
        var activated: String? = null
        show(loaded, FocusTemplatesActions(onActivate = { activated = it }))

        composeRule.onAllNodesWithText(str(R.string.focus_activate)).assertCountEquals(2)
        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_activate, "Sprint Rápido")).performClick()

        assertEquals(DefaultFocusTemplates.quickSprint.id, activated)
    }

    @Test
    fun alTocarEditarYEliminar_seNotificaElIdDeLaPlantilla() {
        var edited: String? = null
        var deleted: String? = null
        show(loaded, FocusTemplatesActions(onEdit = { edited = it }, onRequestDelete = { deleted = it }))

        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_edit, "Sprint Rápido")).performClick()
        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_delete, "Lectura Nocturna")).performClick()

        assertEquals(DefaultFocusTemplates.quickSprint.id, edited)
        assertEquals(DefaultFocusTemplates.nightReading.id, deleted)
    }

    @Test
    fun conUnaSolaPlantilla_noOfreceEliminar() {
        show(FocusTemplatesUiState(isLoading = false, templates = listOf(DefaultFocusTemplates.deepStudy)))

        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_edit, "Estudio Profundo")).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(str(R.string.focus_cd_delete, "Estudio Profundo"))
            .assertCountEquals(0)
    }

    @Test
    fun losBotonesDeAccionTienenAlMenos48dp() {
        show(loaded)

        listOf(R.string.focus_cd_edit, R.string.focus_cd_delete).forEach { id ->
            composeRule.onNodeWithContentDescription(str(id, "Sprint Rápido"))
                .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
                .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_activate, "Sprint Rápido"))
            .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        composeRule.onNodeWithText(str(R.string.focus_create)).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
    }

    @Test
    fun crearNuevoBloque_notificaLaAccion() {
        var created = false
        show(loaded, FocusTemplatesActions(onCreate = { created = true }))

        composeRule.onNodeWithText(str(R.string.focus_create)).performClick()

        assertEquals(true, created)
    }

    @Test
    fun cargando_muestraElIndicadorYNoElBotonDeCrear() {
        show(FocusTemplatesUiState(isLoading = true))

        composeRule.onNodeWithContentDescription(str(R.string.focus_cd_loading)).assertIsDisplayed()
        composeRule.onAllNodesWithText(str(R.string.focus_create)).assertCountEquals(0)
    }

    @Test
    fun conErrorDeLectura_ofreceReintentar() {
        var retried = false
        show(
            FocusTemplatesUiState(isLoading = false, hasLoadError = true),
            FocusTemplatesActions(onRetry = { retried = true }),
        )

        composeRule.onNodeWithText(str(R.string.focus_load_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.action_retry)).performClick()

        assertEquals(true, retried)
    }

    @Test
    fun sinPlantillas_muestraElEstadoVacioYElBotonDeCrear() {
        show(FocusTemplatesUiState(isLoading = false))

        composeRule.onNodeWithText(str(R.string.focus_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_create)).assertIsDisplayed()
    }

    @Test
    fun formulario_muestraErroresYGuardarNotificaLaAccion() {
        var saved = false
        val editor = FocusTemplateEditorState(
            name = "",
            focusMinutesText = "0",
            breakMinutesText = "5",
            nameErrorRes = R.string.focus_error_name_blank,
            focusErrorRes = R.string.focus_error_focus_minutes,
        )
        show(
            loaded.copy(editor = editor),
            FocusTemplatesActions(editor = FocusTemplateEditorActions(onSave = { saved = true })),
        )

        composeRule.onNodeWithText(str(R.string.focus_error_name_blank)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_error_focus_minutes)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_action_save)).performClick()

        assertEquals(true, saved)
    }

    @Test
    fun formulario_mientrasGuarda_desactivaElBotonGuardar() {
        show(loaded.copy(editor = FocusTemplateEditorState(name = "A", focusMinutesText = "25", breakMinutesText = "5", isSaving = true)))

        composeRule.onNodeWithText(str(R.string.focus_action_save)).assertIsNotEnabled()
    }

    @Test
    fun formularioDeEdicion_muestraElTituloDeEditar() {
        show(loaded.copy(editor = FocusTemplateEditorState(templateId = "x", name = "A", focusMinutesText = "25", breakMinutesText = "5")))

        composeRule.onNodeWithText(str(R.string.focus_editor_title_edit)).assertIsDisplayed()
    }

    @Test
    fun confirmacionDeEliminar_conservarYEliminarNotificanSuAccion() {
        var kept = false
        var confirmed = false
        val pending = FocusTemplate("x", "Sprint Rápido", 25, 5, isActive = true)
        show(
            loaded.copy(templatePendingDelete = pending),
            FocusTemplatesActions(onDismissDelete = { kept = true }, onConfirmDelete = { confirmed = true }),
        )

        composeRule.onNodeWithText(str(R.string.focus_delete_active_note)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.focus_delete_keep)).performClick()
        composeRule.onNodeWithText(str(R.string.focus_delete_confirm)).performClick()

        assertEquals(true, kept)
        assertEquals(true, confirmed)
    }

    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
    }
}

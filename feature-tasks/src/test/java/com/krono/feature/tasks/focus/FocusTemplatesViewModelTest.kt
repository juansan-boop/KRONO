package com.krono.feature.tasks.focus

import com.krono.core.domain.focus.ActivateFocusTemplateUseCase
import com.krono.core.domain.focus.CreateFocusTemplateUseCase
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.DeleteFocusTemplateUseCase
import com.krono.core.domain.focus.EditFocusTemplateUseCase
import com.krono.core.domain.focus.FakeFocusTemplateRepository
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.FocusTemplateChanges
import com.krono.core.domain.focus.FocusTemplateError
import com.krono.core.domain.focus.ObserveFocusTemplatesUseCase
import com.krono.feature.tasks.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de [FocusTemplatesViewModel] (F-10): estados de carga, vacío y error, validación del
 * formulario y las acciones de crear, editar, eliminar con confirmación y activar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusTemplatesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeFocusTemplateRepository
    private lateinit var viewModel: FocusTemplatesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeFocusTemplateRepository()
        viewModel = FocusTemplatesViewModel(
            observeTemplates = ObserveFocusTemplatesUseCase(repository),
            createTemplate = CreateFocusTemplateUseCase(repository) { "nueva" },
            editTemplate = EditFocusTemplateUseCase(repository),
            deleteTemplate = DeleteFocusTemplateUseCase(repository),
            activateTemplate = ActivateFocusTemplateUseCase(repository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.subscribe() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private val state get() = viewModel.uiState.value

    private fun editorWith(name: String, focus: String, rest: String) {
        viewModel.onNameChange(name)
        viewModel.onFocusMinutesChange(focus)
        viewModel.onBreakMinutesChange(rest)
    }

    @Test
    fun alInicio_muestraCargando() = runTest {

        assertTrue(state.isLoading)
    }

    @Test
    fun instalacionNueva_muestraLasTresPlantillasPorDefectoConEstudioProfundoActiva() = runTest {
        subscribe()
        advanceUntilIdle()

        assertFalse(state.isLoading)
        assertEquals(listOf("Estudio Profundo", "Lectura Nocturna", "Sprint Rápido"), state.templates.map { it.name })
        assertEquals(listOf("Estudio Profundo"), state.templates.filter { it.isActive }.map { it.name })
        assertEquals(82, state.templates.first().focusPercent)
    }

    @Test
    fun siLaLecturaFalla_muestraErrorYReintentarVuelveAMostrarLasPlantillas() = runTest {
        repository.failure = IllegalStateException("sin permiso")
        subscribe()
        advanceUntilIdle()
        assertTrue(state.hasLoadError)
        assertTrue(state.templates.isEmpty())

        repository.failure = null
        viewModel.onRetry()
        advanceUntilIdle()

        assertFalse(state.hasLoadError)
        assertEquals(3, state.templates.size)
    }

    @Test
    fun crear_abreElFormularioConValoresPorDefecto() = runTest {
        subscribe()
        advanceUntilIdle()

        viewModel.onCreateClick()

        val editor = state.editor!!
        assertNull(editor.templateId)
        assertEquals("", editor.name)
        assertEquals("25", editor.focusMinutesText)
        assertEquals("5", editor.breakMinutesText)
    }

    @Test
    fun guardarNueva_persisteYCierraElFormulario() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("Repaso", "30", "6")

        viewModel.onSave()
        advanceUntilIdle()

        assertNull(state.editor)
        assertEquals(FocusTemplate("nueva", "Repaso", 30, 6, isActive = false), repository.current.single { it.id == "nueva" })
        assertEquals(4, state.templates.size)
    }

    @Test
    fun guardar_nombreVacio_muestraErrorYNoEscribe() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("   ", "30", "6")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(R.string.focus_error_name_blank, state.editor!!.nameErrorRes)
        assertTrue(repository.appliedChanges.isEmpty())
    }

    @Test
    fun guardar_minutosCeroOVacios_muestraErroresPorCampo() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("Repaso", "0", "")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(R.string.focus_error_focus_minutes, state.editor!!.focusErrorRes)
        assertEquals(R.string.focus_error_break_minutes, state.editor!!.breakErrorRes)
        assertNull(state.editor!!.nameErrorRes)
        assertTrue(repository.appliedChanges.isEmpty())
    }

    @Test
    fun escribirEnUnCampo_limpiaSuError() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("", "30", "6")
        viewModel.onSave()

        viewModel.onNameChange("Repaso")

        assertNull(state.editor!!.nameErrorRes)
    }

    @Test
    fun campoDeMinutos_soloAceptaDigitosYTresCaracteres() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()

        viewModel.onFocusMinutesChange("4a5b")
        assertEquals("45", state.editor!!.focusMinutesText)

        viewModel.onFocusMinutesChange("12345")
        assertEquals("123", state.editor!!.focusMinutesText)
    }

    @Test
    fun editar_precargaLosDatosYGuardaLosCambios() = runTest {
        subscribe()
        advanceUntilIdle()
        val sprint = DefaultFocusTemplates.quickSprint

        viewModel.onEditClick(sprint.id)
        assertEquals(sprint.id, state.editor!!.templateId)
        assertEquals("Sprint Rápido", state.editor!!.name)
        assertEquals("25", state.editor!!.focusMinutesText)
        editorWith("Sprint largo", "30", "6")
        viewModel.onSave()
        advanceUntilIdle()

        assertNull(state.editor)
        assertEquals(FocusTemplate(sprint.id, "Sprint largo", 30, 6, isActive = false), repository.current.single { it.id == sprint.id })
    }

    @Test
    fun cancelarElFormulario_loCierraSinEscribir() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()

        viewModel.onDismissEditor()

        assertNull(state.editor)
        assertTrue(repository.appliedChanges.isEmpty())
    }

    @Test
    fun guardarDosVeces_seguidas_soloEscribeUnaVez() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("Repaso", "30", "6")

        viewModel.onSave()
        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(1, repository.appliedChanges.size)
    }

    @Test
    fun activar_cambiaLaPlantillaActivaYSoloHayUna() = runTest {
        subscribe()
        advanceUntilIdle()

        viewModel.onActivate(DefaultFocusTemplates.nightReading.id)
        advanceUntilIdle()

        assertEquals(listOf("Lectura Nocturna"), state.templates.filter { it.isActive }.map { it.name })
        assertEquals("Lectura Nocturna", state.templates.first().name)
    }

    @Test
    fun eliminar_pideConfirmacionYSoloBorraAlConfirmar() = runTest {
        subscribe()
        advanceUntilIdle()

        viewModel.onRequestDelete(DefaultFocusTemplates.quickSprint.id)
        assertEquals("Sprint Rápido", state.templatePendingDelete!!.name)
        assertEquals(3, repository.current.size)
        viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertNull(state.templatePendingDelete)
        assertEquals(2, state.templates.size)
        assertTrue(state.templates.none { it.id == DefaultFocusTemplates.quickSprint.id })
    }

    @Test
    fun eliminar_cancelarConservaLaPlantilla() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onRequestDelete(DefaultFocusTemplates.quickSprint.id)

        viewModel.onDismissDelete()
        advanceUntilIdle()

        assertNull(state.templatePendingDelete)
        assertEquals(3, repository.current.size)
    }

    @Test
    fun eliminarLaActiva_activaOtraYSiguenExistiendoPlantillasActivas() = runTest {
        subscribe()
        advanceUntilIdle()

        viewModel.onRequestDelete(DefaultFocusTemplates.deepStudy.id)
        viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertEquals(2, state.templates.size)
        assertEquals(1, state.templates.count { it.isActive })
    }

    @Test
    fun conUnaSolaPlantilla_noSePuedeEliminar() = runTest {
        subscribe()
        advanceUntilIdle()
        repository.applyChanges(
            FocusTemplateChanges(
                deletedIds = listOf(DefaultFocusTemplates.quickSprint.id, DefaultFocusTemplates.nightReading.id),
            ),
        )
        advanceUntilIdle()
        assertFalse(state.canDelete)

        viewModel.onRequestDelete(DefaultFocusTemplates.deepStudy.id)

        assertNull(state.templatePendingDelete)
    }

    @Test
    fun siLaEscrituraFalla_muestraMensajeYConservaElFormulario() = runTest {
        subscribe()
        advanceUntilIdle()
        viewModel.onCreateClick()
        editorWith("Repaso", "30", "6")
        repository.failure = FocusTemplateError.StorageError("sin permiso")

        viewModel.onSave()
        advanceUntilIdle()

        assertEquals(R.string.focus_error_save, state.messageRes)
        assertNotNull(state.editor)
        assertFalse(state.editor!!.isSaving)
        viewModel.onMessageShown()
        assertNull(state.messageRes)
    }

    @Test
    fun siActivarFalla_muestraMensaje() = runTest {
        subscribe()
        advanceUntilIdle()
        repository.applyChanges(FocusTemplateChanges())
        repository.failure = FocusTemplateError.StorageError("sin permiso")

        viewModel.onActivate(DefaultFocusTemplates.quickSprint.id)
        advanceUntilIdle()

        assertEquals(R.string.focus_error_activate, state.messageRes)
    }
}

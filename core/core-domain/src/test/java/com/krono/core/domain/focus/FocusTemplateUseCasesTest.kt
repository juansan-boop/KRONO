package com.krono.core.domain.focus

import com.krono.core.common.KronoResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de los casos de uso de plantillas de enfoque (F-10 CA-1 a CA-3): siembra por defecto,
 * crear, editar, eliminar y activar, siempre con exactamente una plantilla activa.
 */
class FocusTemplateUseCasesTest {

    private val estudio = FocusTemplate("a", "Estudio Profundo", 45, 10, isActive = true)
    private val sprint = FocusTemplate("b", "Sprint Rápido", 25, 5, isActive = false)
    private val lectura = FocusTemplate("c", "Lectura Nocturna", 60, 15, isActive = false)

    private fun repository(vararg templates: FocusTemplate) = FakeFocusTemplateRepository(templates.toList())

    private fun FakeFocusTemplateRepository.activeIds() = current.filter { it.isActive }.map { it.id }

    // ---- Observar y sembrar (CA-1) ----

    @Test
    fun observar_instalacionNueva_siembraLasTresPlantillasPorDefecto() = runTest {
        val repo = FakeFocusTemplateRepository()

        val result = ObserveFocusTemplatesUseCase(repo)().first()

        val list = (result as KronoResult.Success).data
        assertEquals(DefaultFocusTemplates.all.map { it.id }.toSet(), list.map { it.id }.toSet())
        assertEquals(1, list.count { it.isActive })
        assertEquals("Estudio Profundo", list.single { it.isActive }.name)
    }

    @Test
    fun observar_variasVeces_noDuplicaNiPisaLasPlantillas() = runTest {
        val repo = FakeFocusTemplateRepository()
        val observe = ObserveFocusTemplatesUseCase(repo)
        observe().first()
        repo.applyChanges(FocusTemplateChanges(upserts = listOf(DefaultFocusTemplates.all.first().copy(name = "Editada"))))

        observe().first()

        assertEquals(DefaultFocusTemplates.all.size, repo.current.size)
        assertEquals("Editada", repo.current.first { it.id == DefaultFocusTemplates.all.first().id }.name)
    }

    @Test
    fun observar_ordenaActivaPrimeroYElRestoPorNombre() = runTest {
        val repo = repository(lectura, estudio.copy(isActive = false), sprint.copy(isActive = true))

        val list = (ObserveFocusTemplatesUseCase(repo)().first() as KronoResult.Success).data

        assertEquals(listOf("b", "a", "c"), list.map { it.id })
    }

    @Test
    fun observar_sinNingunaActiva_marcaLaPrimeraPorNombreSoloEnPantalla() = runTest {
        val repo = repository(sprint, lectura)

        val list = (ObserveFocusTemplatesUseCase(repo)().first() as KronoResult.Success).data

        assertEquals(listOf("c"), list.filter { it.isActive }.map { it.id })
    }

    @Test
    fun observar_conVariasActivas_dejaUnaSola() = runTest {
        val repo = repository(estudio, sprint.copy(isActive = true))

        val list = (ObserveFocusTemplatesUseCase(repo)().first() as KronoResult.Success).data

        assertEquals(1, list.count { it.isActive })
    }

    @Test
    fun observar_conFalloAlSembrar_aunAsiEmiteElResultadoDeLaObservacion() = runTest {
        val repo = repository(estudio)
        repo.failure = IllegalStateException("sin permiso")

        val result = ObserveFocusTemplatesUseCase(repo)().first()

        assertTrue(result is KronoResult.Error)
    }

    // ---- Crear (CA-2) ----

    @Test
    fun crear_guardaLaPlantillaInactivaYConNombreSinEspacios() = runTest {
        val repo = repository(estudio, sprint)

        val result = CreateFocusTemplateUseCase(repo) { "nueva" }("  Repaso  ", 30, 5)

        assertEquals(KronoResult.Success(Unit), result)
        val created = repo.current.single { it.id == "nueva" }
        assertEquals(FocusTemplate("nueva", "Repaso", 30, 5, isActive = false), created)
        assertEquals(listOf("a"), repo.activeIds())
    }

    @Test
    fun crear_sinPlantillas_laNuevaQuedaActiva() = runTest {
        val repo = FakeFocusTemplateRepository()

        CreateFocusTemplateUseCase(repo) { "nueva" }("Repaso", 30, 5)

        assertEquals(listOf("nueva"), repo.activeIds())
    }

    @Test
    fun crear_datosInvalidos_devuelveErrorSinEscribir() = runTest {
        val repo = repository(estudio)
        val create = CreateFocusTemplateUseCase(repo) { "nueva" }

        assertEquals(KronoResult.Error(FocusTemplateError.BlankName), create(" ", 30, 5))
        assertEquals(KronoResult.Error(FocusTemplateError.InvalidFocusMinutes), create("A", 0, 5))
        assertEquals(KronoResult.Error(FocusTemplateError.InvalidBreakMinutes), create("A", 30, 0))
        assertTrue(repo.appliedChanges.isEmpty())
    }

    @Test
    fun crear_conFalloDeLectura_devuelveElError() = runTest {
        val repo = repository(estudio)
        repo.failure = IllegalStateException("fallo")

        val result = CreateFocusTemplateUseCase(repo) { "nueva" }("A", 30, 5)

        assertTrue(result is KronoResult.Error)
    }

    // ---- Editar (CA-2) ----

    @Test
    fun editar_cambiaNombreYMinutosSinTocarLaActiva() = runTest {
        val repo = repository(estudio, sprint)

        val result = EditFocusTemplateUseCase(repo)("b", " Sprint largo ", 30, 6)

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals(FocusTemplate("b", "Sprint largo", 30, 6, isActive = false), repo.current.single { it.id == "b" })
        assertEquals(listOf("a"), repo.activeIds())
    }

    @Test
    fun editar_laActiva_conservaQueSigaActiva() = runTest {
        val repo = repository(estudio, sprint)

        EditFocusTemplateUseCase(repo)("a", "Estudio", 50, 10)

        assertEquals(listOf("a"), repo.activeIds())
    }

    @Test
    fun editar_plantillaInexistente_devuelveNoEncontrada() = runTest {
        val repo = repository(estudio)

        val result = EditFocusTemplateUseCase(repo)("zzz", "A", 30, 5)

        assertEquals(KronoResult.Error(FocusTemplateError.TemplateNotFound), result)
    }

    @Test
    fun editar_datosInvalidos_devuelveErrorSinEscribir() = runTest {
        val repo = repository(estudio)

        val result = EditFocusTemplateUseCase(repo)("a", "", 30, 5)

        assertEquals(KronoResult.Error(FocusTemplateError.BlankName), result)
        assertTrue(repo.appliedChanges.isEmpty())
    }

    // ---- Eliminar (CA-2) ----

    @Test
    fun eliminar_unaNoActiva_laQuitaYConservaLaActiva() = runTest {
        val repo = repository(estudio, sprint, lectura)

        val result = DeleteFocusTemplateUseCase(repo)("b")

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals(listOf("a", "c"), repo.current.map { it.id })
        assertEquals(listOf("a"), repo.activeIds())
    }

    @Test
    fun eliminar_laActiva_activaOtraEnLaMismaEscritura() = runTest {
        val repo = repository(estudio, sprint, lectura)

        DeleteFocusTemplateUseCase(repo)("a")

        assertEquals(1, repo.appliedChanges.size)
        assertEquals(listOf("b", "c"), repo.current.map { it.id }.sorted())
        assertEquals(1, repo.activeIds().size)
    }

    @Test
    fun eliminar_laUnicaPlantilla_devuelveErrorYNoEscribe() = runTest {
        val repo = repository(estudio)

        val result = DeleteFocusTemplateUseCase(repo)("a")

        assertEquals(KronoResult.Error(FocusTemplateError.CannotDeleteLastTemplate), result)
        assertTrue(repo.appliedChanges.isEmpty())
        assertEquals(listOf("a"), repo.current.map { it.id })
    }

    @Test
    fun eliminar_inexistente_devuelveNoEncontrada() = runTest {
        val repo = repository(estudio, sprint)

        val result = DeleteFocusTemplateUseCase(repo)("zzz")

        assertEquals(KronoResult.Error(FocusTemplateError.TemplateNotFound), result)
    }

    // ---- Activar (CA-2) ----

    @Test
    fun activar_otra_desactivaLaAnteriorEnUnaSolaEscritura() = runTest {
        val repo = repository(estudio, sprint, lectura)

        val result = ActivateFocusTemplateUseCase(repo)("c")

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals(listOf("c"), repo.activeIds())
        assertEquals(1, repo.appliedChanges.size)
    }

    @Test
    fun activar_laYaActiva_noEscribeNada() = runTest {
        val repo = repository(estudio, sprint)

        val result = ActivateFocusTemplateUseCase(repo)("a")

        assertEquals(KronoResult.Success(Unit), result)
        assertTrue(repo.appliedChanges.isEmpty())
    }

    @Test
    fun activar_conVariasActivasPorUnConflicto_dejaSoloLaElegida() = runTest {
        val repo = repository(estudio, sprint.copy(isActive = true), lectura)

        ActivateFocusTemplateUseCase(repo)("c")

        assertEquals(listOf("c"), repo.activeIds())
    }

    @Test
    fun activar_inexistente_devuelveNoEncontrada() = runTest {
        val repo = repository(estudio)

        val result = ActivateFocusTemplateUseCase(repo)("zzz")

        assertEquals(KronoResult.Error(FocusTemplateError.TemplateNotFound), result)
    }

    @Test
    fun activar_conFallo_devuelveElErrorSinCambiarNada() = runTest {
        val repo = repository(estudio, sprint)
        repo.failure = IllegalStateException("fallo")

        val result = ActivateFocusTemplateUseCase(repo)("b")

        assertTrue(result is KronoResult.Error)
        assertEquals(listOf("a"), repo.activeIds())
    }
}

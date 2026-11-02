package com.krono.core.data.focus

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.FakeAuthRemoteDataSource
import com.krono.core.domain.auth.User
import com.krono.core.domain.focus.ActivateFocusTemplateUseCase
import com.krono.core.domain.focus.DefaultFocusTemplates
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.FocusTemplateChanges
import com.krono.core.domain.focus.FocusTemplateError
import com.krono.core.domain.focus.ObserveFocusTemplatesUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Pruebas de [FirestoreFocusTemplateRepository] (F-10): siembra idempotente, escrituras por lote,
 * espera acotada sin conexión y traducción de errores.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreFocusTemplateRepositoryTest {

    private val user = User(id = "uid-1", email = "estudiante@krono.app")
    private val sprint = FocusTemplate("sprint", "Sprint Rápido", 25, 5, isActive = false)

    private lateinit var auth: FakeAuthRemoteDataSource
    private lateinit var remote: FakeFocusTemplateRemoteDataSource
    private lateinit var repository: FirestoreFocusTemplateRepository

    @Before
    fun setUp() {
        auth = FakeAuthRemoteDataSource()
        remote = FakeFocusTemplateRemoteDataSource()
        repository = FirestoreFocusTemplateRepository(remote, auth)
    }

    @Test
    fun sembrar_sinPlantillas_escribeLasTresPorDefectoEnUnSoloLote() = runTest {
        auth.user.value = user

        val result = repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all)

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals(1, remote.commits.size)
        assertEquals(DefaultFocusTemplates.all.map { it.id }, remote.templatesOf(user.id).map { it.id })
        assertEquals(listOf("Estudio Profundo"), remote.templatesOf(user.id).filter { it.isActive }.map { it.name })
    }

    @Test
    fun sembrar_variasVeces_noDuplicaNiPisaLoEditado() = runTest {
        auth.user.value = user
        repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all)
        repository.applyChanges(
            FocusTemplateChanges(upserts = listOf(DefaultFocusTemplates.quickSprint.copy(name = "Mi sprint"))),
        )

        repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all)

        assertEquals(3, remote.templatesOf(user.id).size)
        assertEquals("Mi sprint", remote.templatesOf(user.id).first { it.id == DefaultFocusTemplates.quickSprint.id }.name)
        assertEquals(2, remote.commits.size)
    }

    @Test
    fun sembrar_conPlantillasExistentes_noEscribeNada() = runTest {
        auth.user.value = user
        remote.given(user.id, listOf(FocusTemplateDto.fromDomain(sprint.copy(isActive = true))))

        repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all)

        assertTrue(remote.commits.isEmpty())
    }

    @Test
    fun sinSesion_lasOperacionesDevuelvenUsuarioNoAutenticado() = runTest {
        assertEquals(
            KronoResult.Error(FocusTemplateError.UnauthenticatedUser),
            repository.applyChanges(FocusTemplateChanges(upserts = listOf(sprint))),
        )
        assertEquals(KronoResult.Error(FocusTemplateError.UnauthenticatedUser), repository.getTemplates())
        assertEquals(
            KronoResult.Error(FocusTemplateError.UnauthenticatedUser),
            repository.seedDefaultsIfEmpty(DefaultFocusTemplates.all),
        )
    }

    @Test
    fun aplicarCambios_escribeUpsertsYBorradosEnUnSoloLote() = runTest {
        auth.user.value = user
        remote.given(user.id, listOf(FocusTemplateDto.fromDomain(sprint), FocusTemplateDto.fromDomain(DefaultFocusTemplates.deepStudy)))

        val result = repository.applyChanges(
            FocusTemplateChanges(upserts = listOf(sprint.copy(isActive = true)), deletedIds = listOf(DefaultFocusTemplates.deepStudy.id)),
        )

        assertEquals(KronoResult.Success(Unit), result)
        assertEquals(1, remote.commits.size)
        assertEquals(listOf(sprint.copy(isActive = true)), remote.templatesOf(user.id).map { it.toDomain() })
    }

    @Test
    fun aplicarCambios_sinConfirmacionDelServidor_devuelveSuccessSinBloquear() = runTest {
        auth.user.value = user
        remote.serverConfirmation = CompletableDeferred()

        val result = backgroundScope.async { repository.applyChanges(FocusTemplateChanges(upserts = listOf(sprint))) }
        advanceTimeBy(OFFLINE_WAIT)
        runCurrent()

        assertTrue(result.isCompleted)
        assertEquals(KronoResult.Success(Unit), result.await())
        assertEquals(listOf(sprint), remote.templatesOf(user.id).map { it.toDomain() })
    }

    @Test
    fun aplicarCambios_conErrorLocal_devuelveErrorDeAlmacenamiento() = runTest {
        auth.user.value = user
        remote.writeFailure = FirebaseFirestoreException("denegado", FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val result = repository.applyChanges(FocusTemplateChanges(upserts = listOf(sprint)))

        assertTrue((result as KronoResult.Error).throwable is FocusTemplateError.StorageError)
    }

    @Test
    fun observar_emiteLasPlantillasDelUsuarioComoDominio() = runTest {
        auth.user.value = user
        remote.given(user.id, listOf(FocusTemplateDto.fromDomain(sprint)))

        repository.observeTemplates().test {
            assertEquals(KronoResult.Success(listOf(sprint)), awaitItem())
            remote.given(user.id, listOf(FocusTemplateDto.fromDomain(sprint), FocusTemplateDto.fromDomain(DefaultFocusTemplates.deepStudy)))
            assertEquals(2, (awaitItem() as KronoResult.Success).data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observar_sinSesion_emiteListaVacia() = runTest {
        assertEquals(KronoResult.Success(emptyList<FocusTemplate>()), repository.observeTemplates().first())
    }

    @Test
    fun observar_siElListenerFalla_emiteErrorTipado() = runTest {
        auth.user.value = user
        remote.listenerFailure = FirebaseFirestoreException("denegado", FirebaseFirestoreException.Code.PERMISSION_DENIED)

        val result = repository.observeTemplates().first()

        assertTrue((result as KronoResult.Error).throwable is FocusTemplateError.StorageError)
    }

    @Test
    fun casosDeUso_instalacionNuevaYActivarOtra_dejaUnaSolaActivaEnLosDatos() = runTest {
        auth.user.value = user
        val observe = ObserveFocusTemplatesUseCase(repository)
        observe().first()

        ActivateFocusTemplateUseCase(repository)(DefaultFocusTemplates.nightReading.id)

        assertEquals(
            listOf(DefaultFocusTemplates.nightReading.id),
            remote.templatesOf(user.id).filter { it.isActive }.map { it.id },
        )
    }

    private companion object {
        /** Más que la espera máxima a la confirmación del servidor, como si no hubiera red. */
        val OFFLINE_WAIT = 3.seconds
    }
}

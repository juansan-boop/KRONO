package com.krono.core.domain.profile

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pruebas de [ObserveUserNameUseCase] (F-13 CA-1: saludo con el nombre del usuario). */
class ObserveUserNameUseCaseTest {

    private val repository = FakeUserNameRepository()
    private val observeUserName = ObserveUserNameUseCase(repository)

    @Test
    fun nombreConEspacios_seEmiteRecortado() = runTest {
        repository.name.value = "  Alexa "

        observeUserName().test {
            assertEquals("Alexa", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nombreEnBlanco_seEmiteComoNulo() = runTest {
        repository.name.value = "   "

        observeUserName().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sinNombre_seEmiteNulo() = runTest {
        observeUserName().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package com.krono.feature.calendar

import app.cash.turbine.test
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Prueba de humo (F-00 CA-3): demuestra que el módulo ejecuta JUnit4, runTest y Turbine. */
class PruebasHumoTest {

    @Test
    fun elModuloEjecutaPruebasConCorrutinasYFlows() = runTest {
        flowOf(1, 2).test {
            assertEquals(1, awaitItem())
            assertEquals(2, awaitItem())
            awaitComplete()
        }
    }
}

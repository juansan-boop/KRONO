package com.krono.feature.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Reemplaza `Dispatchers.Main` (usado por `viewModelScope`) por un dispatcher de prueba. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    /** Dispatcher de prueba; las pruebas lo avanzan para ejecutar las corrutinas pendientes. */
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    /** Instala [dispatcher] como `Dispatchers.Main` antes de cada prueba. */
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    /** Restaura `Dispatchers.Main` al terminar cada prueba. */
    override fun finished(description: Description) = Dispatchers.resetMain()
}

package com.krono.app.navigation

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.navigation.AUTH_GRAPH_ROUTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Prueba de los márgenes de sistema de [KronoShell]: una pantalla con su propio `Scaffold` (como
 * Cronómetro, Enfoque o Tareas) no debe sumar otra vez la barra de estado que ya reserva el
 * Scaffold de `app`, o queda un hueco vacío sobre su contenido.
 */
class KronoInsetsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unaPantallaConScaffoldPropio_noDuplicaElMargenDeLaBarraDeEstado() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
        var statusBarPx by mutableFloatStateOf(0f)
        composeRule.setContent {
            KronoTheme {
                statusBarPx = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
                val controlador = rememberNavController()
                KronoShell(
                    navController = controlador,
                    destinoInicial = RUTA_GRAFO_PRINCIPAL,
                    grafoAuth = { _ ->
                        navigation(startDestination = "auth/falso", route = AUTH_GRAPH_ROUTE) {
                            composable("auth/falso") {}
                        }
                    },
                    ajustes = {},
                    inicio = {
                        Scaffold { padding -> Text(MARCA, Modifier.padding(padding)) }
                    },
                )
            }
        }
        composeRule.waitForIdle()

        val top = composeRule.onNodeWithText(MARCA).fetchSemanticsNode().boundsInRoot.top

        assertTrue("La prueba necesita una barra de estado con alto", statusBarPx > 0f)
        assertEquals("El contenido debe quedar justo bajo la barra de estado", statusBarPx, top, TOLERANCE_PX)
    }

    private companion object {
        const val MARCA = "CONTENIDO_DE_PRUEBA"
        const val TOLERANCE_PX = 2f
    }
}

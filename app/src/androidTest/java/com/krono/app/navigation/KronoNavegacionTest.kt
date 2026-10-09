package com.krono.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.app.R
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.navigation.AUTH_GRAPH_ROUTE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de la navegación base (F-01). El grafo de auth real necesita Hilt, así que se
 * sustituye por uno falso con la misma ruta: lo que se verifica es la navegación de `app`.
 */
class KronoNavegacionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var navController: NavHostController
    private var cierresDeSesion = 0

    private fun texto(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun grafoAuthFalso(): NavGraphBuilder.(() -> Unit) -> Unit = { alAutenticarse ->
        navigation(startDestination = RUTA_LOGIN_FALSA, route = AUTH_GRAPH_ROUTE) {
            composable(RUTA_LOGIN_FALSA) {
                Button(onClick = alAutenticarse) { Text(TEXTO_LOGIN_FALSO) }
            }
        }
    }

    private fun mostrar(inicio: String) {
        composeRule.setContent {
            KronoTheme {
                val controlador = rememberNavController()
                navController = controlador
                KronoShell(
                    navController = controlador,
                    destinoInicial = inicio,
                    grafoAuth = grafoAuthFalso(),
                    ajustes = { alCerrarSesion -> AjustesDePrueba(alCerrarSesion) },
                )
            }
        }
    }

    @Composable
    private fun AjustesDePrueba(alCerrarSesion: () -> Unit) {
        var contador by rememberSaveable { mutableIntStateOf(0) }
        var confirmacion by rememberSaveable { mutableStateOf(false) }
        Button(onClick = { contador++ }) { Text("$TEXTO_CONTADOR $contador") }
        PantallaAjustes(
            confirmacionVisible = confirmacion,
            cerrando = false,
            hayError = false,
            alSolicitarCierre = { confirmacion = true },
            alCancelarCierre = { confirmacion = false },
            alConfirmarCierre = {
                confirmacion = false
                cierresDeSesion++
                alCerrarSesion()
            },
        )
    }

    /** Pestaña localizada por "Ir a <sección>" en el árbol fusionado, el mismo que consume TalkBack. */
    private fun pestana(@StringRes etiqueta: Int) = composeRule.onNodeWithContentDescription(
        label = texto(R.string.nav_ir_a, texto(etiqueta)),
    )

    private fun tocarPestana(@StringRes etiqueta: Int) {
        pestana(etiqueta).performClick()
    }

    private fun hayEntradaEnLaPila(ruta: String): Boolean =
        runCatching { navController.getBackStackEntry(ruta) }.isSuccess

    @Test
    fun alAbrirConSesionLaBarraMuestraLasCuatroPestanasYArrancaEnInicio() {
        mostrar(RUTA_GRAFO_PRINCIPAL)

        listOf(R.string.nav_inicio, R.string.nav_tareas, R.string.nav_calendario, R.string.nav_ajustes)
            .forEach { pestana(it).assertIsDisplayed() }
        composeRule.onNodeWithText(texto(R.string.seccion_proximamente)).assertIsDisplayed()
        assertEquals(KronoDestination.Inicio.route, navController.currentDestination?.route)
    }

    @Test
    fun alTocarCadaPestanaSeMuestraSuPantallaProvisional() {
        mostrar(RUTA_GRAFO_PRINCIPAL)

        listOf(
            R.string.nav_tareas to KronoDestination.Tareas,
            R.string.nav_calendario to KronoDestination.Calendario,
            R.string.nav_ajustes to KronoDestination.Ajustes,
            R.string.nav_inicio to KronoDestination.Inicio,
        ).forEach { (etiqueta, destino) ->
            tocarPestana(etiqueta)
            composeRule.waitForIdle()
            assertEquals(destino.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun alVolverAUnaPestanaSeConservaSuEstado() {
        mostrar(RUTA_GRAFO_PRINCIPAL)
        tocarPestana(R.string.nav_ajustes)
        composeRule.onNodeWithText("$TEXTO_CONTADOR 0").performClick()
        composeRule.onNodeWithText("$TEXTO_CONTADOR 1").assertIsDisplayed()

        tocarPestana(R.string.nav_tareas)
        tocarPestana(R.string.nav_ajustes)

        composeRule.onNodeWithText("$TEXTO_CONTADOR 1").assertIsDisplayed()
    }

    @Test
    fun sinSesionSeMuestraElLoginSinBarraInferior() {
        mostrar(AUTH_GRAPH_ROUTE)

        composeRule.onNodeWithText(TEXTO_LOGIN_FALSO).assertIsDisplayed()
        pestana(R.string.nav_inicio)
            .assertDoesNotExist()
    }

    @Test
    fun alAutenticarseEntraAlFlujoPrincipalYAtrasNoVuelveAlLogin() {
        mostrar(AUTH_GRAPH_ROUTE)

        composeRule.onNodeWithText(TEXTO_LOGIN_FALSO).performClick()
        composeRule.waitForIdle()

        pestana(R.string.nav_inicio)
            .assertIsDisplayed()
        assertEquals(KronoDestination.Inicio.route, navController.currentDestination?.route)
        assertFalse(hayEntradaEnLaPila(AUTH_GRAPH_ROUTE))
    }

    @Test
    fun elBotonCerrarSesionAbreElDialogoDeConfirmacion() {
        mostrar(RUTA_GRAFO_PRINCIPAL)
        tocarPestana(R.string.nav_ajustes)

        composeRule.onNodeWithText(texto(R.string.cerrar_sesion)).performClick()

        composeRule.onNodeWithText(texto(R.string.cerrar_sesion_dialogo_titulo)).assertIsDisplayed()
        composeRule.onNodeWithText(texto(R.string.cancelar)).assertIsDisplayed()
    }

    @Test
    fun alCancelarElDialogoSeCierraYSeSigueEnAjustes() {
        mostrar(RUTA_GRAFO_PRINCIPAL)
        tocarPestana(R.string.nav_ajustes)
        composeRule.onNodeWithText(texto(R.string.cerrar_sesion)).performClick()

        composeRule.onNodeWithText(texto(R.string.cancelar)).performClick()

        composeRule.onNodeWithText(texto(R.string.cerrar_sesion_dialogo_titulo)).assertDoesNotExist()
        assertEquals(KronoDestination.Ajustes.route, navController.currentDestination?.route)
        assertEquals(0, cierresDeSesion)
    }

    @Test
    fun alConfirmarSeVuelveAlLoginSinBarraYAtrasNoRegresaALaApp() {
        mostrar(RUTA_GRAFO_PRINCIPAL)
        tocarPestana(R.string.nav_ajustes)
        composeRule.onNodeWithText(texto(R.string.cerrar_sesion)).performClick()

        composeRule.onNode(hasText(texto(R.string.cerrar_sesion)) and hasAnyAncestor(isDialog())).performClick()
        composeRule.waitForIdle()

        assertEquals(1, cierresDeSesion)
        composeRule.onNodeWithText(TEXTO_LOGIN_FALSO).assertIsDisplayed()
        pestana(R.string.nav_inicio)
            .assertDoesNotExist()
        assertFalse(hayEntradaEnLaPila(RUTA_GRAFO_PRINCIPAL))
        assertTrue(hayEntradaEnLaPila(AUTH_GRAPH_ROUTE))
    }

    private companion object {
        const val RUTA_LOGIN_FALSA = "auth/login-falso"
        const val TEXTO_LOGIN_FALSO = "LOGIN_FALSO"
        const val TEXTO_CONTADOR = "Contador"
    }
}

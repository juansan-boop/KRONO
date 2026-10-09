package com.krono.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.login.LoginScreen
import com.krono.feature.auth.login.LoginUiState
import com.krono.feature.auth.register.RegisterScreen
import com.krono.feature.auth.register.RegisterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil

/**
 * Las pantallas de acceso deben seguir siendo legibles con la fuente al 200 %
 * (ajuste de accesibilidad del sistema): sin recortes ni palabras partidas por la mitad.
 */
class EscalaDeFuenteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun mostrarConEscalaMaxima(contenido: @Composable () -> Unit) {
        composeRule.setContent {
            val densidad = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(densidad.density, ESCALA_MAXIMA)) {
                KronoTheme { contenido() }
            }
        }
    }

    private fun mostrarLogin() = mostrarConEscalaMaxima {
        LoginScreen(
            state = LoginUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
            onForgotPassword = {},
            onCreateAccount = {},
        )
    }

    private fun mostrarRegistro() = mostrarConEscalaMaxima {
        RegisterScreen(
            state = RegisterUiState(),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmationChange = {},
            onTogglePasswordVisibility = {},
            onToggleConfirmationVisibility = {},
            onSubmit = {},
            onBack = {},
            onSignIn = {},
            onContinue = {},
        )
    }

    private fun SemanticsNodeInteraction.disposicionDelTexto(): TextLayoutResult {
        val resultados = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(resultados)
        return resultados.first()
    }

    private fun textoSinPalabrasPartidas(texto: String) {
        val disposicion = composeRule.onNodeWithText(texto, useUnmergedTree = true).disposicionDelTexto()
        for (linea in 1 until disposicion.lineCount) {
            val inicio = disposicion.getLineStart(linea)
            assertTrue(
                "'$texto' parte una palabra en la línea $linea",
                disposicion.layoutInput.text.text[inicio - 1].isWhitespace(),
            )
        }
    }

    @Test
    fun enElLoginLaPalabraKronoNoSeRecortaNiSePartePorLaMitad() {
        mostrarLogin()

        val disposicion = composeRule.onNodeWithText(str(R.string.auth_brand_name), useUnmergedTree = true)
            .disposicionDelTexto()

        assertEquals(1, disposicion.lineCount)
        assertTrue("La palabra KRONO se recorta: no cabe completa", ceil(disposicion.multiParagraph.maxIntrinsicWidth) <= disposicion.size.width)
    }

    @Test
    fun enElLoginElEnlaceCrearCuentaSoloSeParteEntrePalabras() {
        mostrarLogin()

        textoSinPalabrasPartidas(str(R.string.auth_login_create_account))
        textoSinPalabrasPartidas(str(R.string.auth_login_no_account))
    }

    @Test
    fun enElRegistroElEnlaceIniciarSesionQuedaDentroDelAnchoDeLaPantalla() {
        mostrarRegistro()

        val enlace = composeRule.onNodeWithText(str(R.string.auth_register_sign_in), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val pantalla = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

        assertTrue("El enlace sobresale por la derecha", enlace.right <= pantalla.right)
        assertTrue("El enlace sobresale por la izquierda", enlace.left >= pantalla.left)
        textoSinPalabrasPartidas(str(R.string.auth_register_sign_in))
    }

    private companion object {
        /** Escala máxima de fuente que ofrece Android en Ajustes de accesibilidad. */
        const val ESCALA_MAXIMA = 2f
    }
}

package com.krono.feature.auth.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import com.krono.core.ui.brand.KronoSymbolTestTag
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.str
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submits = 0
    private var emailTyped = ""
    private var toggles = 0
    private var forgotClicks = 0
    private var createClicks = 0

    private fun show(state: LoginUiState) {
        composeRule.setContent {
            KronoTheme {
                LoginScreen(
                    state = state,
                    onEmailChange = { emailTyped = it },
                    onPasswordChange = {},
                    onTogglePasswordVisibility = { toggles++ },
                    onSubmit = { submits++ },
                    onForgotPassword = { forgotClicks++ },
                    onCreateAccount = { createClicks++ },
                )
            }
        }
    }

    @Test
    fun conCamposVaciosMuestraElMensajeDeCamposObligatorios() {
        show(LoginUiState(error = AuthMessage.EmptyFields))

        composeRule.onNodeWithText(str(R.string.auth_error_empty_fields)).assertIsDisplayed()
    }

    @Test
    fun conCredencialesInvalidasMuestraUnMensajeClaro() {
        show(LoginUiState(email = "ana@correo.com", password = "x", error = AuthMessage.InvalidCredentials))

        composeRule.onNodeWithText(str(R.string.auth_error_invalid_credentials)).assertIsDisplayed()
    }

    @Test
    fun alEnviarLlamaAOnSubmit() {
        show(LoginUiState())

        composeRule.onNodeWithText(str(R.string.auth_login_submit)).performClick()

        assertEquals(1, submits)
    }

    @Test
    fun mientrasCargaElBotonMuestraProgresoYEstaDeshabilitado() {
        show(LoginUiState(email = "ana@correo.com", password = "krono2026!", isLoading = true))

        composeRule.onNodeWithText(str(R.string.auth_login_loading)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_login_loading)).assertIsNotEnabled()
        composeRule.onNodeWithText(str(R.string.auth_login_loading)).performClick()
        assertEquals(0, submits)
    }

    @Test
    fun escribirEnElCorreoNotificaElCambio() {
        show(LoginUiState())

        composeRule.onNodeWithText(str(R.string.auth_field_email)).performTextInput("ana@correo.com")

        assertEquals("ana@correo.com", emailTyped)
    }

    @Test
    fun elBotonDeVisibilidadTieneDescripcionYAlternaLaContrasena() {
        show(LoginUiState())

        composeRule.onNodeWithContentDescription(str(R.string.auth_show_password)).performClick()

        assertEquals(1, toggles)
    }

    @Test
    fun conLaContrasenaVisibleOfreceOcultarla() {
        show(LoginUiState(isPasswordVisible = true))

        composeRule.onNodeWithContentDescription(str(R.string.auth_hide_password)).assertIsDisplayed()
    }

    @Test
    fun losEnlacesLlevanARecuperarYCrearCuenta() {
        show(LoginUiState())

        composeRule.onNodeWithText(str(R.string.auth_login_forgot_password)).performClick()
        composeRule.onNodeWithText(str(R.string.auth_login_create_account)).performClick()

        assertEquals(1, forgotClicks)
        assertEquals(1, createClicks)
    }

    @Test
    fun muestraElLogoDeKronoConSimboloYPalabra() {
        show(LoginUiState())

        composeRule.onNodeWithTag(KronoSymbolTestTag, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_brand_name)).assertIsDisplayed()
    }
}

package com.krono.feature.auth.forgot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import com.krono.core.ui.brand.KronoSymbolTestTag
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.str
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForgotPasswordScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submits = 0
    private var backs = 0

    private fun show(state: ForgotPasswordUiState) {
        composeRule.setContent {
            KronoTheme {
                ForgotPasswordScreen(
                    state = state,
                    onEmailChange = {},
                    onSubmit = { submits++ },
                    onBack = { backs++ },
                )
            }
        }
    }

    @Test
    fun sinConexionMuestraElMensajeDeSinConexion() {
        show(ForgotPasswordUiState(email = "ana@correo.com", error = AuthMessage.NoConnection))

        composeRule.onNodeWithText(str(R.string.auth_error_no_connection)).assertIsDisplayed()
    }

    @Test
    fun conCorreoMalFormadoMuestraElMensajeDeCorreoInvalido() {
        show(ForgotPasswordUiState(email = "ana@correo", error = AuthMessage.InvalidEmail))

        composeRule.onNodeWithText(str(R.string.auth_error_invalid_email)).assertIsDisplayed()
    }

    @Test
    fun alEnviarLlamaAOnSubmit() {
        show(ForgotPasswordUiState(email = "ana@correo.com"))

        composeRule.onNodeWithText(str(R.string.auth_forgot_submit)).performClick()

        assertEquals(1, submits)
    }

    @Test
    fun mientrasCargaElBotonEstaDeshabilitado() {
        show(ForgotPasswordUiState(email = "ana@correo.com", isLoading = true))

        composeRule.onNodeWithText(str(R.string.auth_forgot_loading)).assertIsNotEnabled()
    }

    @Test
    fun conExitoMuestraLaPantallaDeEnlaceEnviadoConElTextoNeutro() {
        show(ForgotPasswordUiState(email = "ana@correo.com", isSent = true))

        composeRule.onNodeWithText(str(R.string.auth_forgot_success_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_forgot_success_body)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_forgot_back_to_login)).performClick()

        assertEquals(1, backs)
    }

    @Test
    fun muestraElLogoDeKronoConSimboloYPalabra() {
        show(ForgotPasswordUiState())

        composeRule.onNodeWithTag(KronoSymbolTestTag, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_brand_name)).assertIsDisplayed()
    }
}

package com.krono.feature.auth.register

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.onNodeWithTag
import com.krono.core.ui.brand.KronoSymbolTestTag
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.str
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var submits = 0
    private var continues = 0
    private var signIns = 0

    private fun show(state: RegisterUiState) {
        composeRule.setContent {
            KronoTheme {
                RegisterScreen(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmationChange = {},
                    onTogglePasswordVisibility = {},
                    onToggleConfirmationVisibility = {},
                    onSubmit = { submits++ },
                    onBack = {},
                    onSignIn = { signIns++ },
                    onContinue = { continues++ },
                )
            }
        }
    }

    @Test
    fun noTieneCampoDeNombreSoloCorreoContrasenaYConfirmacion() {
        show(RegisterUiState())

        composeRule.onNodeWithText(str(R.string.auth_field_email)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_field_password)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_field_confirm_password)).assertIsDisplayed()
    }

    @Test
    fun losIndicadoresReflejanLosRequisitosCumplidosYPendientes() {
        show(RegisterUiState(password = "abcdefgh"))

        val longitud = str(R.string.auth_register_requirement_length)
        val numero = str(R.string.auth_register_requirement_digit)
        composeRule.onNodeWithContentDescription(str(R.string.auth_register_requirement_met, longitud)).assertExists()
        composeRule.onNodeWithContentDescription(str(R.string.auth_register_requirement_unmet, numero)).assertExists()
    }

    @Test
    fun siLaConfirmacionNoCoincideMuestraElAviso() {
        show(
            RegisterUiState(
                email = "ana@correo.com",
                password = "krono2026!",
                confirmation = "krono2026?",
                submitAttempted = true,
                error = AuthMessage.PasswordsDoNotMatch,
            ),
        )

        composeRule.onNodeWithText(str(R.string.auth_error_passwords_mismatch)).assertIsDisplayed()
    }

    @Test
    fun conCorreoYaRegistradoMuestraElError() {
        show(RegisterUiState(email = "ana@correo.com", error = AuthMessage.EmailAlreadyRegistered))

        composeRule.onNodeWithText(str(R.string.auth_error_email_already_registered)).assertIsDisplayed()
    }

    @Test
    fun mientrasCargaElBotonEstaDeshabilitado() {
        show(RegisterUiState(isLoading = true))

        composeRule.onNodeWithText(str(R.string.auth_register_loading)).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun alEnviarLlamaAOnSubmitYElEnlaceVuelveAlLogin() {
        show(RegisterUiState())

        composeRule.onNodeWithText(str(R.string.auth_register_submit)).performScrollTo().performClick()
        composeRule.onNodeWithText(str(R.string.auth_register_sign_in)).performScrollTo().performClick()

        assertEquals(1, submits)
        assertEquals(1, signIns)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun enterEnLaConfirmacionEnviaElFormularioSinVolverAlLogin() {
        show(RegisterUiState(email = "ana@correo.com", password = "krono", confirmation = "kron"))

        composeRule.onNodeWithText(str(R.string.auth_field_confirm_password)).performClick()
        composeRule.onNodeWithText(str(R.string.auth_field_confirm_password)).performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        assertEquals(1, submits)
        assertEquals(0, signIns)
    }

    @Test
    fun conLaCuentaCreadaMuestraElExitoYPermiteContinuar() {
        show(RegisterUiState(isRegistered = true))

        composeRule.onNodeWithText(str(R.string.auth_register_success_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_register_success_continue)).performClick()

        assertEquals(1, continues)
    }

    @Test
    fun laBarraSuperiorMuestraElSimboloYLaMarca() {
        show(RegisterUiState())

        composeRule.onNodeWithTag(KronoSymbolTestTag, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.auth_brand_name)).assertIsDisplayed()
    }

    @Test
    fun elExitoDelRegistroConservaElSimboloEnLaBarraSuperior() {
        show(RegisterUiState(isRegistered = true))

        composeRule.onNodeWithTag(KronoSymbolTestTag, useUnmergedTree = true).assertIsDisplayed()
    }
}

package com.krono.feature.profile.security

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.profile.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas de UI de [SecurityZoneScreen] (F-21): los errores de las operaciones destructivas deben
 * verse dentro del diálogo abierto y "Cancelar" debe responder aunque haya una operación en curso.
 */
class SecurityZoneScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var dismissedDeleteDialog = 0
    private var dismissedResetDialog = 0

    private fun str(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun show(state: SecurityZoneUiState) {
        composeRule.setContent {
            KronoTheme {
                SecurityZoneScreen(
                    state = state,
                    onBackClick = {},
                    onSelectAction = {},
                    onContinueToConfirmation = {},
                    onDismissResetDialog = { dismissedResetDialog++ },
                    onConfirmResetData = {},
                    onDismissDeleteDialog = { dismissedDeleteDialog++ },
                    onReauthPasswordChange = {},
                    onTogglePasswordVisibility = {},
                    onConfirmDeleteAccount = {},
                )
            }
        }
    }

    private fun deleteDialogState(error: SecurityZoneError? = null, isDeleting: Boolean = false) =
        SecurityZoneUiState(
            selectedAction = SecurityActionType.DELETE_ACCOUNT,
            showDeleteAccountDialog = true,
            reauthPassword = "incorrecta",
            isDeletingAccount = isDeleting,
            error = error,
        )

    @Test
    fun contrasenaIncorrectaMuestraElErrorDentroDelDialogoDeEliminarCuenta() {
        show(deleteDialogState(error = SecurityZoneError.WrongPassword))

        composeRule
            .onNode(hasText(str(R.string.security_error_wrong_password)) and hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun sinRedMuestraElErrorDentroDelDialogoDeRestablecer() {
        show(
            SecurityZoneUiState(
                selectedAction = SecurityActionType.RESET_DATA,
                showResetConfirmationDialog = true,
                error = SecurityZoneError.NetworkUnavailable,
            ),
        )

        composeRule
            .onNode(hasText(str(R.string.security_error_network_reset)) and hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun cancelarRespondeMientrasSeEliminaLaCuenta() {
        show(deleteDialogState(isDeleting = true))

        composeRule.onNodeWithText(str(R.string.security_dialog_delete_cancel)).assertIsEnabled().performClick()

        assertEquals(1, dismissedDeleteDialog)
    }

    @Test
    fun cancelarRespondeMientrasSeRestablecenLosDatos() {
        show(
            SecurityZoneUiState(
                showResetConfirmationDialog = true,
                isResettingData = true,
            ),
        )

        composeRule.onNodeWithText(str(R.string.security_dialog_delete_cancel)).assertIsEnabled().performClick()

        assertEquals(1, dismissedResetDialog)
    }
}

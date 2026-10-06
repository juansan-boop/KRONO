package com.krono.feature.auth.register

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.ui.AuthFooterPrompt
import com.krono.feature.auth.ui.AuthLayout
import com.krono.feature.auth.ui.AuthPrimaryButton
import com.krono.feature.auth.ui.AuthTextField
import com.krono.feature.auth.ui.AuthTopBar
import com.krono.feature.auth.ui.FormErrorMessage
import com.krono.feature.auth.ui.GlassBadge
import com.krono.feature.auth.ui.stringRes

@Composable
internal fun RegisterRoute(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    RegisterScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onToggleConfirmationVisibility = viewModel::onToggleConfirmationVisibility,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        onSignIn = onBack,
        onContinue = onRegistered,
    )
}

/**
 * Crear cuenta (F-23): correo, contraseña y confirmación (sin nombre, que va en el
 * onboarding). Referencia: `/design/crear_cuenta_lumina_style_1`,
 * `..._error_contrase_as_no_coinciden` y `crear_cuenta_xito_lumina_style_1`.
 */
@Composable
internal fun RegisterScreen(
    state: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmationVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isRegistered) {
        // Con la cuenta creada ya hay sesión: "atrás" también continúa, no vuelve al formulario.
        BackHandler(onBack = onContinue)
        RegisterSuccessContent(onContinue = onContinue, modifier = modifier)
    } else {
        RegisterFormContent(
            state = state,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onConfirmationChange = onConfirmationChange,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            onToggleConfirmationVisibility = onToggleConfirmationVisibility,
            onSubmit = onSubmit,
            onBack = onBack,
            onSignIn = onSignIn,
            modifier = modifier,
        )
    }
}

@Composable
private fun RegisterFormContent(
    state: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmationVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val missingFields = state.error == AuthMessage.EmptyFields
    val emailMessage = state.error?.takeIf {
        it == AuthMessage.InvalidEmail || it == AuthMessage.EmailAlreadyRegistered
    }
    // Errores que no pertenecen a un campo concreto se muestran sobre el botón.
    val generalMessage = state.error?.takeIf {
        it == AuthMessage.EmptyFields || it == AuthMessage.WeakPassword || it == AuthMessage.Unexpected
    }
    val submit = {
        focusManager.clearFocus()
        onSubmit()
    }

    AuthLayout(modifier = modifier) {
        AuthTopBar(onBack = onBack)
        Spacer(Modifier.height(KronoSpacing.xl))
        Text(
            text = stringResource(R.string.auth_register_title),
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurface,
        )
        Spacer(Modifier.height(KronoSpacing.sm))
        Text(
            text = stringResource(R.string.auth_register_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
        )
        Spacer(Modifier.height(KronoSpacing.xl))

        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_field_email),
            placeholder = stringResource(R.string.auth_field_email_placeholder),
            isError = emailMessage != null || (missingFields && state.email.isBlank()),
            supportingMessage = emailMessage?.let { stringResource(it.stringRes()) },
            keyboardType = KeyboardType.Email,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )
        Spacer(Modifier.height(KronoSpacing.md))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_field_password),
            isError = (state.submitAttempted && !state.requirements.isSatisfied) ||
                (missingFields && state.password.isEmpty()),
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            isPassword = true,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
        )
        Spacer(Modifier.height(KronoSpacing.sm))
        PasswordRequirementsList(state = state)
        Spacer(Modifier.height(KronoSpacing.md))
        AuthTextField(
            value = state.confirmation,
            onValueChange = onConfirmationChange,
            label = stringResource(R.string.auth_field_confirm_password),
            isError = state.showPasswordsMismatch || (missingFields && state.confirmation.isEmpty()),
            supportingMessage = if (state.showPasswordsMismatch) {
                stringResource(R.string.auth_error_passwords_mismatch)
            } else {
                null
            },
            imeAction = ImeAction.Go,
            onImeAction = submit,
            isPassword = true,
            isPasswordVisible = state.isConfirmationVisible,
            onTogglePasswordVisibility = onToggleConfirmationVisibility,
        )
        Spacer(Modifier.height(KronoSpacing.lg))

        generalMessage?.let {
            FormErrorMessage(message = stringResource(it.stringRes()))
            Spacer(Modifier.height(KronoSpacing.md))
        }
        AuthPrimaryButton(
            text = stringResource(R.string.auth_register_submit),
            loadingText = stringResource(R.string.auth_register_loading),
            isLoading = state.isLoading,
            onClick = submit,
        )
        Spacer(Modifier.height(KronoSpacing.lg))
        AuthFooterPrompt(
            prompt = stringResource(R.string.auth_register_has_account),
            action = stringResource(R.string.auth_register_sign_in),
            onAction = onSignIn,
        )
    }
}

@Composable
private fun PasswordRequirementsList(state: RegisterUiState) {
    val requirements = state.requirements
    Column(
        modifier = Modifier.padding(horizontal = KronoSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        RequirementRow(R.string.auth_register_requirement_length, requirements.minLength, state.submitAttempted)
        RequirementRow(R.string.auth_register_requirement_digit, requirements.hasDigit, state.submitAttempted)
        RequirementRow(R.string.auth_register_requirement_special, requirements.hasSpecialChar, state.submitAttempted)
    }
}

/** Indicador en vivo: neutro mientras escribe, cyan al cumplirse y rojo si falta tras un intento. */
@Composable
private fun RequirementRow(label: Int, isMet: Boolean, submitAttempted: Boolean) {
    val text = stringResource(label)
    val description = stringResource(
        if (isMet) R.string.auth_register_requirement_met else R.string.auth_register_requirement_unmet,
        text,
    )
    val color = when {
        isMet -> SecondaryCyan
        submitAttempted -> ErrorColor
        else -> OnSurfaceVariant
    }
    val icon = when {
        isMet -> Icons.Outlined.CheckCircle
        submitAttempted -> Icons.Outlined.Cancel
        else -> Icons.Outlined.RadioButtonUnchecked
    }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(KronoSizes.iconSmall))
        Spacer(Modifier.width(KronoSpacing.sm))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun RegisterSuccessContent(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    AuthLayout(modifier = modifier) {
        AuthTopBar(onBack = null)
        Spacer(Modifier.height(KronoSpacing.xl))
        Text(
            text = stringResource(R.string.auth_register_success_title),
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurface,
        )
        Spacer(Modifier.height(KronoSpacing.sm))
        Text(
            text = stringResource(R.string.auth_register_success_body),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
        )
        Spacer(Modifier.height(KronoSpacing.xxl))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassBadge(content = {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconLarge),
                )
            })
        }
        Spacer(Modifier.height(KronoSpacing.xxl))
        AuthPrimaryButton(text = stringResource(R.string.auth_register_success_continue), onClick = onContinue)
    }
}

@Preview
@Composable
private fun RegisterScreenMismatchPreview() {
    KronoTheme {
        RegisterScreen(
            state = RegisterUiState(
                email = "tu@correo.com",
                password = "krono",
                confirmation = "kron",
                submitAttempted = true,
                error = AuthMessage.WeakPassword,
            ),
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
}

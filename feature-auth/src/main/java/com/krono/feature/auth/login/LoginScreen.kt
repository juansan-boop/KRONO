package com.krono.feature.auth.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.ui.AuthFooterPrompt
import com.krono.feature.auth.ui.AuthHeader
import com.krono.feature.auth.ui.AuthLayout
import com.krono.feature.auth.ui.AuthPrimaryButton
import com.krono.feature.auth.ui.AuthTextField
import com.krono.feature.auth.ui.AuthTextLink
import com.krono.feature.auth.ui.FormErrorMessage
import com.krono.feature.auth.ui.stringRes

/** Punto de entrada con estado: conecta [LoginViewModel] con la pantalla y sus eventos. */
@Composable
internal fun LoginRoute(
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.LoggedIn -> onLoggedIn()
            }
        }
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = onForgotPassword,
        onCreateAccount = onCreateAccount,
    )
}

/** Iniciar sesión (F-22). Referencia: `/design/iniciar_sesi_n_lumina_style_1` y `..._error_de_validaci_n`. */
@Composable
internal fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val missingFields = state.error == AuthMessage.EmptyFields
    val badCredentials = state.error == AuthMessage.InvalidCredentials

    AuthLayout(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle),
        )
        Spacer(Modifier.height(KronoSpacing.xl))

        state.error?.let { error ->
            FormErrorMessage(message = stringResource(error.stringRes()))
            Spacer(Modifier.height(KronoSpacing.md))
        }

        AuthTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_field_email),
            placeholder = stringResource(R.string.auth_field_email_placeholder),
            isError = (missingFields && state.email.isBlank()) || state.error == AuthMessage.InvalidEmail || badCredentials,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )
        Spacer(Modifier.height(KronoSpacing.md))
        AuthTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_field_password),
            isError = (missingFields && state.password.isEmpty()) || badCredentials,
            imeAction = ImeAction.Go,
            onImeAction = {
                focusManager.clearFocus()
                onSubmit()
            },
            isPassword = true,
            isPasswordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            AuthTextLink(text = stringResource(R.string.auth_login_forgot_password), onClick = onForgotPassword)
        }
        Spacer(Modifier.height(KronoSpacing.md))

        AuthPrimaryButton(
            text = stringResource(R.string.auth_login_submit),
            loadingText = stringResource(R.string.auth_login_loading),
            isLoading = state.isLoading,
            onClick = {
                focusManager.clearFocus()
                onSubmit()
            },
        )
        Spacer(Modifier.height(KronoSpacing.xl))

        AuthFooterPrompt(
            prompt = stringResource(R.string.auth_login_no_account),
            action = stringResource(R.string.auth_login_create_account),
            onAction = onCreateAccount,
        )
    }
}

@Preview
@Composable
private fun LoginScreenErrorPreview() {
    KronoTheme {
        LoginScreen(
            state = LoginUiState(error = AuthMessage.EmptyFields),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onSubmit = {},
            onForgotPassword = {},
            onCreateAccount = {},
        )
    }
}

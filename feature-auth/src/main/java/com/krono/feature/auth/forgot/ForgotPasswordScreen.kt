package com.krono.feature.auth.forgot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R
import com.krono.feature.auth.ui.AuthHeader
import com.krono.feature.auth.ui.AuthLayout
import com.krono.feature.auth.ui.AuthPrimaryButton
import com.krono.feature.auth.ui.AuthTextField
import com.krono.feature.auth.ui.AuthTextLink
import com.krono.feature.auth.ui.GlassBadge
import com.krono.feature.auth.ui.stringRes

@Composable
internal fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
    )
}

/**
 * Recuperar contraseña (F-24), simulado mientras no haya backend. Referencia:
 * `/design/recuperar_contrase_a_estilo_lumina_glass_1`, `..._error_correo_no_encontrado`
 * y `recuperar_contrase_a_xito_lumina_style_1`.
 */
@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isSent) {
        ForgotPasswordSuccessContent(onBack = onBack, modifier = modifier)
        return
    }
    // Ocultar el teclado sin quitar el foco (ver RegisterScreen: ENTER de teclado físico).
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        keyboard?.hide()
        onSubmit()
    }

    AuthLayout(modifier = modifier) {
        AuthHeader(
            title = stringResource(R.string.auth_forgot_title),
            subtitle = stringResource(R.string.auth_forgot_subtitle),
        )
        Spacer(Modifier.height(KronoSpacing.xl))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(KronoSpacing.lg),
        ) {
            AuthTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.auth_field_email),
                placeholder = stringResource(R.string.auth_field_email_placeholder),
                isError = state.error != null,
                supportingMessage = state.error?.let { stringResource(it.stringRes()) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
                onImeAction = submit,
            )
            Spacer(Modifier.height(KronoSpacing.md))
            AuthPrimaryButton(
                text = stringResource(R.string.auth_forgot_submit),
                loadingText = stringResource(R.string.auth_forgot_loading),
                isLoading = state.isLoading,
                onClick = submit,
            )
        }
        Spacer(Modifier.height(KronoSpacing.lg))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthTextLink(text = stringResource(R.string.auth_forgot_back_to_login), onClick = onBack)
        }
    }
}

@Composable
private fun ForgotPasswordSuccessContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    AuthLayout(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassBadge(content = {
                Icon(
                    imageVector = Icons.Outlined.MarkEmailRead,
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconLarge),
                )
            })
        }
        Spacer(Modifier.height(KronoSpacing.lg))
        Text(
            text = stringResource(R.string.auth_forgot_success_title),
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(KronoSpacing.sm))
        Text(
            text = stringResource(R.string.auth_forgot_success_body),
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(KronoSpacing.xl))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(KronoSpacing.lg),
        ) {
            Text(
                text = stringResource(R.string.auth_forgot_success_simulated),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(KronoSpacing.md))
            AuthPrimaryButton(
                text = stringResource(R.string.auth_forgot_back_to_login),
                onClick = onBack,
                showArrow = false,
            )
        }
    }
}

@Preview
@Composable
private fun ForgotPasswordErrorPreview() {
    KronoTheme {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "ejemplo@correo.com", error = AuthMessage.EmailNotRegistered),
            onEmailChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}

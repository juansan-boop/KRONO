package com.krono.feature.auth.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHighest
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.auth.AuthMessage
import com.krono.feature.auth.R

/** Contenedor de las pantallas de acceso: centrado, con scroll y ancho máximo de formulario. */
@Composable
internal fun AuthLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = KronoSizes.formMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.xl),
            content = content,
        )
    }
}

/** Logo de KRONO sobre una superficie glass. */
@Composable
internal fun BrandLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(KronoSizes.brandLogo)
            .glassSurface(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.HourglassTop,
            contentDescription = stringResource(R.string.auth_logo_description),
            tint = PrimaryFixedDim,
            modifier = Modifier.size(KronoSizes.iconLarge),
        )
    }
}

/** Ícono destacado dentro de una superficie glass (pantallas de éxito). */
@Composable
internal fun GlassBadge(
    content: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(KronoSizes.successBadge)
            .glassSurface(cornerRadius = KronoSizes.successBadge / 2),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Encabezado centrado: logo, título y subtítulo. */
@Composable
internal fun AuthHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandLogo()
        Spacer(Modifier.height(KronoSpacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(KronoSpacing.sm))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Barra superior con "volver" y la marca (pantallas de registro). */
@Composable
internal fun AuthTopBar(onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.auth_back),
                    tint = OnSurface,
                )
            }
            Spacer(Modifier.width(KronoSpacing.sm))
        }
        Icon(
            imageVector = Icons.Outlined.HourglassTop,
            contentDescription = null,
            tint = PrimaryFixedDim,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
        Spacer(Modifier.width(KronoSpacing.sm))
        Text(
            text = stringResource(R.string.auth_brand_name),
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface,
        )
    }
}

/**
 * Campo de formulario Lumina Glass. Si [isPassword] es verdadero, agrega el botón
 * de mostrar u ocultar con su descripción accesible.
 */
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onTogglePasswordVisibility: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingMessage?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = stringResource(
                            if (isPasswordVisible) R.string.auth_hide_password else R.string.auth_show_password,
                        ),
                    )
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface,
            errorTextColor = OnSurface,
            focusedContainerColor = SurfaceContainerLow,
            unfocusedContainerColor = SurfaceContainerLow,
            errorContainerColor = SurfaceContainerLow,
            cursorColor = PrimaryFixedDim,
            errorCursorColor = ErrorColor,
            focusedBorderColor = PrimaryFixedDim,
            unfocusedBorderColor = OutlineVariant,
            errorBorderColor = ErrorColor,
            focusedLabelColor = PrimaryFixedDim,
            unfocusedLabelColor = OnSurfaceVariant,
            errorLabelColor = ErrorColor,
            focusedPlaceholderColor = Outline,
            unfocusedPlaceholderColor = Outline,
            focusedTrailingIconColor = OnSurfaceVariant,
            unfocusedTrailingIconColor = OnSurfaceVariant,
            errorTrailingIconColor = ErrorColor,
            errorSupportingTextColor = ErrorColor,
        ),
    )
}

/** Botón primario con estado de carga: muestra progreso y queda deshabilitado (sin doble envío). */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    loadingText: String = text,
    showArrow: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(KronoSizes.controlHeight),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryMagenta,
            contentColor = OnSurface,
            disabledContainerColor = SurfaceContainerHighest,
            disabledContentColor = OnSurfaceVariant,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(KronoSizes.iconSmall),
                color = OnSurfaceVariant,
                strokeWidth = KronoSizes.progressStroke,
            )
            Spacer(Modifier.width(KronoSpacing.sm))
            Text(text = loadingText, style = MaterialTheme.typography.titleMedium)
        } else {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            if (showArrow) {
                Spacer(Modifier.width(KronoSpacing.sm))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
            }
        }
    }
}

/** Enlace de texto con área táctil de 48dp. */
@Composable
internal fun AuthTextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = KronoSizes.minTouchTarget),
        colors = ButtonDefaults.textButtonColors(contentColor = PrimaryFixedDim),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

/** "¿No tienes una cuenta? Crear una cuenta" y similares. */
@Composable
internal fun AuthFooterPrompt(
    prompt: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = prompt, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
        AuthTextLink(text = action, onClick = onAction)
    }
}

/** Mensaje de error general del formulario; se anuncia a lectores de pantalla. */
@Composable
internal fun FormErrorMessage(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = ErrorColor,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Spacer(Modifier.width(KronoSpacing.sm))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
    }
}

@StringRes
internal fun AuthMessage.stringRes(): Int = when (this) {
    AuthMessage.EmptyFields -> R.string.auth_error_empty_fields
    AuthMessage.InvalidEmail -> R.string.auth_error_invalid_email
    AuthMessage.WeakPassword -> R.string.auth_error_weak_password
    AuthMessage.PasswordsDoNotMatch -> R.string.auth_error_passwords_mismatch
    AuthMessage.InvalidCredentials -> R.string.auth_error_invalid_credentials
    AuthMessage.EmailAlreadyRegistered -> R.string.auth_error_email_already_registered
    AuthMessage.EmailNotRegistered -> R.string.auth_error_email_not_registered
    AuthMessage.Unexpected -> R.string.auth_error_unexpected
}

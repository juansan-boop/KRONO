package com.krono.feature.profile.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.OnError
import com.krono.core.ui.theme.OnPrimary
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainer
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.profile.R

/**
 * Punto de entrada stateful para la pantalla de Zona de Seguridad (F-21 CA-1, CA-2).
 *
 * Conecta [SecurityZoneViewModel] con la pantalla stateless [SecurityZoneScreen] y
 * maneja los eventos de navegación y resultado.
 *
 * @param onBackClick Retorno a la pantalla anterior (Ajustes).
 * @param onAccountDeleted Redirección a la pantalla de Auth al confirmarse el borrado de cuenta.
 * @param viewModel Instancia inyectada mediante Hilt.
 */
@Composable
fun SecurityZoneRoute(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: SecurityZoneViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SecurityZoneEvent.DataResetSuccess -> { /* El estado local expone isResetSuccessful */ }
                SecurityZoneEvent.AccountDeleted -> onAccountDeleted()
                SecurityZoneEvent.NavigateBack -> onBackClick()
            }
        }
    }

    SecurityZoneScreen(
        state = state,
        onBackClick = viewModel::onBackClick,
        onSelectAction = viewModel::onSelectAction,
        onContinueToConfirmation = viewModel::onContinueToConfirmation,
        onDismissResetDialog = viewModel::onDismissResetDialog,
        onConfirmResetData = viewModel::onConfirmResetData,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onReauthPasswordChange = viewModel::onReauthPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onConfirmDeleteAccount = viewModel::onConfirmDeleteAccount,
    )
}

/**
 * Pantalla stateless de Zona de Seguridad (F-21 CA-1, CA-2).
 *
 * Diseñada bajo el Lumina Glass System utilizando exclusivamente tokens de `:core:core-ui`.
 * Permite seleccionar el nivel de limpieza deseado y solicita confirmación explícita
 * en un segundo paso para garantizar la máxima seguridad en operaciones destructivas.
 */
@Composable
fun SecurityZoneScreen(
    state: SecurityZoneUiState,
    onBackClick: () -> Unit,
    onSelectAction: (SecurityActionType) -> Unit,
    onContinueToConfirmation: () -> Unit,
    onDismissResetDialog: () -> Unit,
    onConfirmResetData: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onReauthPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onConfirmDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorText = state.error?.let { securityErrorText(it, state.selectedAction) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KronoSizes.formMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.lg),
        ) {
            // 1. Barra superior con placa "ZONA DE RIESGO"
            SecurityZoneTopBar(onBackClick = onBackClick)

            // 2. Cabecera visual de advertencia
            SecurityZoneHeroSection()

            // 3. Tarjetas de opciones (Paso 1)
            SecurityZoneOptionsGroup(
                selectedAction = state.selectedAction,
                onSelectAction = onSelectAction,
                enabled = !state.isOperationInProgress,
            )

            // 4. Nota de seguridad informativa
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KronoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.Error),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Text(
                    text = stringResource(R.string.security_zone_info_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }

            // Banner de éxito si los datos fueron restablecidos (CA-1)
            AnimatedVisibility(
                visible = state.isResetSuccessful,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    shape = RoundedCornerShape(KronoSpacing.sm),
                    color = SecondaryCyan.copy(alpha = 0.15f),
                    border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(KronoSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                    ) {
                        Icon(
                            painter = painterResource(KronoIcons.CheckCircle),
                            contentDescription = null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(KronoSizes.iconMedium),
                        )
                        Text(
                            text = stringResource(R.string.security_dialog_reset_success),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                        )
                    }
                }
            }

            Spacer(Modifier.height(KronoSpacing.xs))

            // 5. Botones de acción principales
            SecurityZoneFooterActions(
                onContinueClick = onContinueToConfirmation,
                onCancelClick = onBackClick,
                enabled = !state.isOperationInProgress,
            )
        }

        // Diálogo Paso 2: Confirmación de Restablecimiento de Datos (CA-1)
        if (state.showResetConfirmationDialog) {
            ResetDataConfirmationDialog(
                isResetting = state.isResettingData,
                errorText = errorText,
                onDismiss = onDismissResetDialog,
                onConfirm = onConfirmResetData,
            )
        }

        // Diálogo Paso 2: Eliminación de Cuenta con Reautenticación sensible (CA-2)
        if (state.showDeleteAccountDialog) {
            DeleteAccountConfirmationDialog(
                password = state.reauthPassword,
                isPasswordEmpty = state.isReauthPasswordEmpty,
                errorText = errorText,
                isPasswordVisible = state.isPasswordVisible,
                isDeleting = state.isDeletingAccount,
                onPasswordChange = onReauthPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onDismiss = onDismissDeleteDialog,
                onConfirm = onConfirmDeleteAccount,
            )
        }
    }
}

/**
 * Barra superior de navegación con placa ZONA DE RIESGO.
 */
@Composable
private fun SecurityZoneTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(KronoSizes.minTouchTarget),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.ArrowBack),
                    contentDescription = stringResource(R.string.security_zone_cd_back),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
            }
            Text(
                text = stringResource(R.string.security_zone_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        }

        Surface(
            shape = RoundedCornerShape(KronoSpacing.md),
            color = ErrorColor.copy(alpha = 0.12f),
            border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.35f)),
        ) {
            Text(
                text = stringResource(R.string.security_zone_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = ErrorColor,
                modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
            )
        }
    }
}

/**
 * Hero visual con ícono centrado y títulos explicativos.
 */
@Composable
private fun SecurityZoneHeroSection(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(KronoSizes.brandLogo - KronoSpacing.md)
                .clip(CircleShape)
                .background(ErrorColor.copy(alpha = 0.12f))
                .border(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(KronoIcons.Cancel),
                contentDescription = stringResource(R.string.security_zone_cd_warning_icon),
                tint = ErrorColor,
                modifier = Modifier.size(KronoSizes.brandSymbol),
            )
        }

        Text(
            text = stringResource(R.string.security_zone_hero_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(R.string.security_zone_hero_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

/**
 * Opciones seleccionables del primer paso (Restablecer datos o Eliminar cuenta).
 */
@Composable
private fun SecurityZoneOptionsGroup(
    selectedAction: SecurityActionType,
    onSelectAction: (SecurityActionType) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        // Opción 1: Restablecer configuración y datos (CA-1)
        SecurityOptionCard(
            isSelected = selectedAction == SecurityActionType.RESET_DATA,
            onClick = { onSelectAction(SecurityActionType.RESET_DATA) },
            title = stringResource(R.string.security_zone_reset_opt_title),
            badgeText = stringResource(R.string.security_zone_reset_opt_badge),
            badgeColor = SecondaryCyan,
            description = stringResource(R.string.security_zone_reset_opt_desc),
            accentColor = SecondaryCyan,
            contentDescription = stringResource(R.string.security_zone_cd_select_reset),
            enabled = enabled,
        )

        // Opción 2: Eliminar cuenta y perfil completo (CA-2)
        SecurityOptionCard(
            isSelected = selectedAction == SecurityActionType.DELETE_ACCOUNT,
            onClick = { onSelectAction(SecurityActionType.DELETE_ACCOUNT) },
            title = stringResource(R.string.security_zone_delete_opt_title),
            badgeText = stringResource(R.string.security_zone_delete_opt_badge),
            badgeColor = ErrorColor,
            description = stringResource(R.string.security_zone_delete_opt_desc),
            accentColor = ErrorColor,
            contentDescription = stringResource(R.string.security_zone_cd_select_delete),
            enabled = enabled,
        )
    }
}

/**
 * Tarjeta individual para seleccionar la acción de seguridad.
 */
@Composable
private fun SecurityOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    title: String,
    badgeText: String,
    badgeColor: androidx.compose.ui.graphics.Color,
    description: String,
    accentColor: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) accentColor else OutlineVariant.copy(alpha = 0.40f)
    val backgroundColor = if (isSelected) accentColor.copy(alpha = 0.08f) else SurfaceContainerLow

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(KronoSpacing.md),
        color = backgroundColor,
        border = BorderStroke(KronoGlass.borderWidth, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
    ) {
        Row(
            modifier = Modifier.padding(KronoSpacing.md),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        ) {
            // Indicador de radio personalizado accesible
            Box(
                modifier = Modifier
                    .padding(top = KronoSpacing.xs / 2)
                    .size(KronoSizes.iconMedium - 4.dp)
                    .clip(CircleShape)
                    .border(
                        KronoGlass.borderWidth * 2,
                        if (isSelected) accentColor else OutlineVariant,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(KronoSizes.iconSmall - 6.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = RoundedCornerShape(KronoSpacing.xs),
                        color = badgeColor.copy(alpha = 0.15f),
                        border = BorderStroke(KronoGlass.borderWidth, badgeColor.copy(alpha = 0.35f)),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 3),
                        )
                    }
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Acciones de pie de página (Paso 1).
 */
@Composable
private fun SecurityZoneFooterActions(
    onContinueClick: () -> Unit,
    onCancelClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Button(
            onClick = onContinueClick,
            enabled = enabled,
            shape = RoundedCornerShape(KronoSpacing.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryMagenta,
                contentColor = OnPrimary,
                disabledContainerColor = PrimaryMagenta.copy(alpha = 0.40f),
            ),
            contentPadding = PaddingValues(horizontal = KronoSpacing.lg),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.security_zone_action_continue),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Icon(
                    painter = painterResource(KronoIcons.ArrowForward),
                    contentDescription = null,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
            }
        }

        TextButton(
            onClick = onCancelClick,
            enabled = enabled,
            shape = RoundedCornerShape(KronoSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.minTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.security_zone_action_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
            )
        }
    }
}

/**
 * Diálogo de confirmación del Paso 2 para restablecer configuración y datos (CA-1).
 */
@Composable
private fun ResetDataConfirmationDialog(
    isResetting: Boolean,
    errorText: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.security_dialog_reset_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.md)) {
                Text(
                    text = stringResource(R.string.security_dialog_reset_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
                errorText?.let { SecurityErrorBanner(text = it) }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryCyan,
                    contentColor = DeepMidnight,
                ),
                shape = RoundedCornerShape(KronoSpacing.sm),
                modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
            ) {
                if (isResetting) {
                    CircularProgressIndicator(
                        strokeWidth = KronoSizes.progressStroke,
                        color = DeepMidnight,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(Modifier.size(KronoSpacing.xs))
                    Text(stringResource(R.string.security_dialog_reset_in_progress))
                } else {
                    Text(stringResource(R.string.security_dialog_reset_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.security_dialog_delete_cancel),
                    color = OnSurfaceVariant,
                )
            }
        },
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(KronoSpacing.lg),
    )
}

/**
 * Diálogo de confirmación destructiva del Paso 2 para eliminar cuenta con reautenticación (CA-2).
 */
@Composable
private fun DeleteAccountConfirmationDialog(
    password: String,
    isPasswordEmpty: Boolean,
    errorText: String?,
    isPasswordVisible: Boolean,
    isDeleting: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.Cancel),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Text(
                    text = stringResource(R.string.security_dialog_delete_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                Text(
                    text = stringResource(R.string.security_dialog_delete_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                ) {
                    Text(
                        text = stringResource(R.string.security_dialog_delete_reauth_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnSurface,
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = KronoSizes.controlHeight),
                        singleLine = true,
                        enabled = !isDeleting,
                        isError = isPasswordEmpty || errorText != null,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.security_dialog_delete_reauth_placeholder),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Outline,
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = onTogglePasswordVisibility,
                                modifier = Modifier.size(KronoSizes.minTouchTarget),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPasswordVisible) KronoIcons.VisibilityOff else KronoIcons.Visibility,
                                    ),
                                    contentDescription = stringResource(
                                        if (isPasswordVisible) {
                                            R.string.security_dialog_cd_hide_password
                                        } else {
                                            R.string.security_dialog_cd_show_password
                                        },
                                    ),
                                    tint = OnSurfaceVariant,
                                    modifier = Modifier.size(KronoSizes.iconMedium),
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceContainerLow,
                            unfocusedContainerColor = SurfaceContainerLow,
                            disabledContainerColor = SurfaceContainerLow,
                            errorContainerColor = SurfaceContainerLow,
                            focusedBorderColor = ErrorColor,
                            unfocusedBorderColor = OutlineVariant,
                            errorBorderColor = ErrorColor,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                        ),
                        shape = RoundedCornerShape(KronoSpacing.sm),
                    )

                    if (isPasswordEmpty) {
                        Text(
                            text = stringResource(R.string.security_dialog_delete_reauth_error_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorColor,
                            modifier = Modifier.padding(start = KronoSpacing.xs),
                        )
                    }
                }

                errorText?.let { SecurityErrorBanner(text = it) }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor,
                    contentColor = OnError,
                ),
                shape = RoundedCornerShape(KronoSpacing.sm),
                modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        strokeWidth = KronoSizes.progressStroke,
                        color = OnError,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(Modifier.size(KronoSpacing.xs))
                    Text(stringResource(R.string.security_dialog_delete_in_progress))
                } else {
                    Text(stringResource(R.string.security_dialog_delete_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
            ) {
                Text(
                    text = stringResource(R.string.security_dialog_delete_cancel),
                    color = OnSurfaceVariant,
                )
            }
        },
        containerColor = SurfaceContainerHigh,
        shape = RoundedCornerShape(KronoSpacing.lg),
    )
}

@Preview(name = "Zona de Seguridad - Paso 1", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun SecurityZoneScreenPreview() {
    KronoTheme {
        SecurityZoneScreen(
            state = SecurityZoneUiState(),
            onBackClick = {},
            onSelectAction = {},
            onContinueToConfirmation = {},
            onDismissResetDialog = {},
            onConfirmResetData = {},
            onDismissDeleteDialog = {},
            onReauthPasswordChange = {},
            onTogglePasswordVisibility = {},
            onConfirmDeleteAccount = {},
        )
    }
}

/**
 * Banner de error mostrado dentro de los diálogos de confirmación, donde el usuario sí lo ve.
 */
@Composable
private fun SecurityErrorBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(KronoSpacing.sm),
        color = ErrorColor.copy(alpha = 0.15f),
        border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.40f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(KronoSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        ) {
            Icon(
                painter = painterResource(KronoIcons.Error),
                contentDescription = null,
                tint = ErrorColor,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurface,
            )
        }
    }
}

/**
 * Traduce un [SecurityZoneError] al copy en español de `strings.xml`, según la [action] en curso.
 */
@Composable
private fun securityErrorText(error: SecurityZoneError, action: SecurityActionType): String {
    val isReset = action == SecurityActionType.RESET_DATA
    return when (error) {
        SecurityZoneError.WrongPassword -> stringResource(R.string.security_error_wrong_password)
        SecurityZoneError.NetworkUnavailable -> stringResource(
            if (isReset) R.string.security_error_network_reset else R.string.security_error_network_delete,
        )
        SecurityZoneError.Timeout -> stringResource(R.string.security_error_timeout)
        SecurityZoneError.Unexpected -> stringResource(
            if (isReset) R.string.security_error_generic_reset else R.string.security_error_generic_delete,
        )
    }
}

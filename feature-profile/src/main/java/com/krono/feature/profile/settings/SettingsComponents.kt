package com.krono.feature.profile.settings

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerHighest
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.profile.R
import com.krono.feature.profile.ui.rememberAsyncAvatarBitmap

/**
 * Cabecera superior de la pantalla de Ajustes con botón de volver atrás y placa CORE v2.4.
 *
 * @param onBackClick Acción al presionar volver.
 * @param modifier Modificador de diseño.
 */
@Composable
internal fun SettingsTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
                    contentDescription = stringResource(R.string.profile_cd_back),
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
            }
            Text(
                text = stringResource(R.string.profile_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
        }

        // Placa indicadora CORE v2.4 con punto parpadeante
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "core_pulse",
        )

        Surface(
            shape = RoundedCornerShape(KronoSpacing.md),
            color = SecondaryCyan.copy(alpha = 0.10f),
            border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.35f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Box(
                    modifier = Modifier
                        .size(KronoSpacing.xs + KronoSpacing.xs / 2)
                        .clip(CircleShape)
                        .background(SecondaryCyan)
                        .alpha(pulseAlpha),
                )
                Text(
                    text = stringResource(R.string.profile_settings_core_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryCyan,
                )
            }
        }
    }
}

/**
 * Tarjeta de identidad del usuario en vidrio Lumina Glass (F-19).
 *
 * Muestra el avatar con halo degradado, estado en línea, nombre, correo,
 * píldora de rol ("Estudiante y Trabajadora") y sub-barra de sincronización KRONO AI.
 *
 * @param state Estado visual del perfil.
 * @param onEditProfileClick Acción para editar la información del perfil.
 * @param modifier Modificador de diseño.
 */
@Composable
internal fun ProfileHeader(
    state: SettingsUiState,
    onEditProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = KronoSpacing.lg)
            .padding(KronoSpacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                // Avatar con halo activo Lumina Glass
                Box(
                    modifier = Modifier.size(KronoSizes.brandLogo),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(KronoSizes.brandLogo - KronoSpacing.md)
                            .clip(RoundedCornerShape(KronoSpacing.md))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryMagenta, SecondaryCyan),
                                ),
                            )
                            .padding(KronoGlass.borderWidth * 2)
                            .clip(RoundedCornerShape(KronoSpacing.sm + KronoSpacing.xs))
                            .background(SurfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bitmap = rememberAsyncAvatarBitmap(state.avatarUri)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = stringResource(R.string.profile_cd_avatar),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text(
                                text = state.userInitials,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryFixedDim,
                            )
                        }
                    }

                    // Indicador de estado en línea
                    Box(
                        modifier = Modifier
                            .size(KronoSpacing.sm + KronoSpacing.xs)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(SecondaryCyan)
                            .border(KronoGlass.borderWidth * 2, DeepMidnight, CircleShape),
                    )
                }

                // Información textual del perfil
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = state.userName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = OnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = onEditProfileClick,
                            modifier = Modifier.size(KronoSizes.minTouchTarget),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.profile_cd_edit_profile),
                                tint = PrimaryFixedDim,
                                modifier = Modifier.size(KronoSizes.iconSmall),
                            )
                        }
                    }

                    Text(
                        text = state.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(KronoSpacing.xs))

                    // Píldora de rol
                    Surface(
                        shape = RoundedCornerShape(KronoSpacing.xs),
                        color = PrimaryMagenta.copy(alpha = 0.15f),
                        border = BorderStroke(KronoGlass.borderWidth, PrimaryMagenta.copy(alpha = 0.35f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(KronoSpacing.xs + KronoSpacing.xs / 2)
                                    .clip(CircleShape)
                                    .background(SecondaryCyan),
                            )
                            Text(
                                text = state.userRole,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryFixedDim,
                            )
                        }
                    }
                }
            }

            // Sub-barra de Sincronización KRONO AI
            HorizontalDivider(color = OutlineVariant.copy(alpha = 0.4f), thickness = KronoGlass.borderWidth)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                ) {
                    Box(
                        modifier = Modifier
                            .size(KronoSpacing.sm)
                            .clip(CircleShape)
                            .background(SecondaryCyan),
                    )
                    Text(
                        text = stringResource(R.string.profile_ai_sync_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(KronoSpacing.xs),
                    color = SecondaryCyan.copy(alpha = 0.12f),
                    border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.30f)),
                ) {
                    Text(
                        text = stringResource(R.string.profile_ai_sync_active),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryCyan,
                        modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
                    )
                }
            }
        }
    }
}

/**
 * Fila interactiva con interruptor ([Switch]) estilizado para Lumina Glass.
 *
 * @param title Título principal del ajuste.
 * @param description Subtítulo explicativo.
 * @param checked Estado booleano del interruptor.
 * @param onCheckedChange Callback al alternar el interruptor.
 * @param modifier Modificador de diseño.
 */
@Composable
internal fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch, onClick = { onCheckedChange(!checked) })
            .padding(vertical = KronoSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }

        Spacer(Modifier.width(KronoSpacing.sm))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnSurface,
                checkedTrackColor = PrimaryMagenta,
                uncheckedThumbColor = OnSurfaceVariant,
                uncheckedTrackColor = SurfaceContainerHighest,
                uncheckedBorderColor = OutlineVariant,
            ),
        )
    }
}

/**
 * Tarjeta interactiva para la Zona de Seguridad y purga de datos (F-21).
 *
 * @param onClick Acción al presionar la tarjeta.
 * @param modifier Modificador de diseño.
 */
@Composable
internal fun SecurityZoneCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KronoSpacing.md))
            .background(ErrorContainer.copy(alpha = 0.20f))
            .border(
                width = KronoGlass.borderWidth,
                color = ErrorColor.copy(alpha = 0.40f),
                shape = RoundedCornerShape(KronoSpacing.md),
            )
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(KronoSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(KronoSizes.brandSymbolSmall)
                    .clip(RoundedCornerShape(KronoSpacing.sm))
                    .background(ErrorContainer)
                    .border(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.35f), RoundedCornerShape(KronoSpacing.sm)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(KronoIcons.Error),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
            }

            Text(
                text = stringResource(R.string.settings_security_zone_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = ErrorColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            painter = painterResource(KronoIcons.ArrowForward),
            contentDescription = stringResource(R.string.settings_cd_security_zone),
            tint = ErrorColor,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
    }
}

/**
 * Diálogo oficial y definitivo de confirmación de cierre de sesión (F-25 CA-4).
 *
 * Ofrece la acción segura "Permanecer en KRONO" y la acción crítica "Cerrar Sesión",
 * con resumen del usuario y verificación de sincronización.
 *
 * @param state Estado visual del perfil.
 * @param onDismissRequest Acción para cancelar y permanecer en KRONO.
 * @param onConfirmLogout Acción para confirmar y ejecutar el cierre de sesión.
 */
@Composable
internal fun LogoutConfirmationDialog(
    state: SettingsUiState,
    onDismissRequest: () -> Unit,
    onConfirmLogout: () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (!state.isLoggingOut) onDismissRequest() },
        properties = DialogProperties(dismissOnBackPress = !state.isLoggingOut, dismissOnClickOutside = !state.isLoggingOut),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = KronoSpacing.lg)
                .padding(KronoSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                // Ícono de peligro con halo
                Box(
                    modifier = Modifier
                        .size(KronoSizes.controlHeight + KronoSpacing.sm)
                        .clip(RoundedCornerShape(KronoSpacing.md))
                        .background(ErrorContainer.copy(alpha = 0.6f))
                        .border(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.4f), RoundedCornerShape(KronoSpacing.md)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(KronoIcons.Logout),
                        contentDescription = stringResource(R.string.logout_dialog_cd_icon),
                        tint = ErrorColor,
                        modifier = Modifier.size(KronoSizes.iconLarge),
                    )
                }

                // Título del diálogo
                Text(
                    text = stringResource(R.string.logout_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                )

                // Mensaje explicativo
                Text(
                    text = stringResource(R.string.logout_dialog_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                // Mini tarjeta de estado del usuario sincronizado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KronoSpacing.md))
                        .background(SurfaceContainerLow)
                        .border(KronoGlass.borderWidth, OutlineVariant.copy(alpha = 0.6f), RoundedCornerShape(KronoSpacing.md))
                        .padding(KronoSpacing.md),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(KronoSizes.brandSymbolSmall)
                                    .clip(RoundedCornerShape(KronoSpacing.xs))
                                    .background(PrimaryMagenta.copy(alpha = 0.35f))
                                    .border(KronoGlass.borderWidth, PrimaryFixedDim.copy(alpha = 0.4f), RoundedCornerShape(KronoSpacing.xs)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val bitmap = rememberAsyncAvatarBitmap(state.avatarUri)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Text(
                                        text = state.userInitials,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryFixedDim,
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.userName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = state.userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OnSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.3f), thickness = KronoGlass.borderWidth)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_ai_sync_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = OnSurfaceVariant,
                            )
                            Text(
                                text = state.syncStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan,
                            )
                        }
                    }
                }

                if (state.logoutErrorMessage != null) {
                    Text(
                        text = state.logoutErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorColor,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(KronoSpacing.xs))

                // Botón Seguro Primario: Permanecer en KRONO
                Button(
                    onClick = onDismissRequest,
                    enabled = !state.isLoggingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = RoundedCornerShape(KronoSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryFixedDim,
                        contentColor = DeepMidnight,
                    ),
                ) {
                    Icon(
                        painter = painterResource(KronoIcons.CheckCircle),
                        contentDescription = null,
                        tint = DeepMidnight,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Spacer(Modifier.width(KronoSpacing.sm))
                    Text(
                        text = stringResource(R.string.logout_dialog_stay),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Botón Crítico: Cerrar Sesión
                Button(
                    onClick = onConfirmLogout,
                    enabled = !state.isLoggingOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = RoundedCornerShape(KronoSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorContainer.copy(alpha = 0.35f),
                        contentColor = ErrorColor,
                        disabledContainerColor = SurfaceContainerHighest,
                        disabledContentColor = OnSurfaceVariant,
                    ),
                    border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.5f)),
                ) {
                    if (state.isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(KronoSizes.iconSmall),
                            color = ErrorColor,
                            strokeWidth = KronoSizes.progressStroke,
                        )
                        Spacer(Modifier.width(KronoSpacing.sm))
                        Text(
                            text = stringResource(R.string.logout_dialog_in_progress),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        Icon(
                            painter = painterResource(KronoIcons.Logout),
                            contentDescription = null,
                            tint = ErrorColor,
                            modifier = Modifier.size(KronoSizes.iconSmall),
                        )
                        Spacer(Modifier.width(KronoSpacing.sm))
                        Text(
                            text = stringResource(R.string.logout_dialog_confirm),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Diálogo informativo con las plantillas predefinidas de concentración de KRONO (F-10 / F-20).
 *
 * Muestra los tres presets oficiales del sistema:
 * 1. Estudio Profundo (45 min concentración / 10 min descanso).
 * 2. Sprint Rápido (25 min concentración / 5 min descanso).
 * 3. Lectura Nocturna (60 min concentración / 15 min descanso).
 *
 * @param onDismissRequest Acción para cerrar el diálogo.
 */
@Composable
internal fun FocusTemplatesDialog(
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassSurface(cornerRadius = KronoSpacing.lg)
                .padding(KronoSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .size(KronoSizes.controlHeight)
                        .clip(RoundedCornerShape(KronoSpacing.md))
                        .background(SecondaryCyan.copy(alpha = 0.15f))
                        .border(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.4f), RoundedCornerShape(KronoSpacing.md)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(KronoIcons.Checklist),
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                }

                Text(
                    text = stringResource(R.string.focus_templates_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = stringResource(R.string.focus_templates_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                ) {
                    FocusTemplateItemCard(
                        title = stringResource(R.string.focus_template_deep_work_title),
                        description = stringResource(R.string.focus_template_deep_work_desc),
                        badge = stringResource(R.string.focus_template_deep_work_badge),
                    )
                    FocusTemplateItemCard(
                        title = stringResource(R.string.focus_template_quick_sprint_title),
                        description = stringResource(R.string.focus_template_quick_sprint_desc),
                        badge = stringResource(R.string.focus_template_quick_sprint_badge),
                    )
                    FocusTemplateItemCard(
                        title = stringResource(R.string.focus_template_night_reading_title),
                        description = stringResource(R.string.focus_template_night_reading_desc),
                        badge = stringResource(R.string.focus_template_night_reading_badge),
                    )
                }

                Spacer(Modifier.height(KronoSpacing.xs))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = RoundedCornerShape(KronoSpacing.md),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryMagenta,
                        contentColor = OnSurface,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.focus_templates_dialog_understood),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusTemplateItemCard(
    title: String,
    description: String,
    badge: String,
) {
    Surface(
        shape = RoundedCornerShape(KronoSpacing.md),
        color = SurfaceContainerLow,
        border = BorderStroke(KronoGlass.borderWidth, OutlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
            Surface(
                shape = RoundedCornerShape(KronoSpacing.xs),
                color = SecondaryCyan.copy(alpha = 0.12f),
                border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.35f)),
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCyan,
                    modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
                )
            }
        }
    }
}

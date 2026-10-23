package com.krono.feature.profile.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.profile.R

/**
 * Pantalla principal stateless de Ajustes y Perfil (F-19, F-20, F-25 CA-4).
 *
 * Estructurada en secciones de Lumina Glass:
 * 1. Cabecera con botón volver y placa de build.
 * 2. Tarjeta de identidad del perfil del usuario ([ProfileHeader]).
 * 3. Ritmo y Descanso (Inteligencia Circadiana y concentración).
 * 4. Configuración de Sesiones (Plantillas de enfoque).
 * 5. Preferencias del Sistema (Alertas prioritarias P1 y tema visual).
 * 6. Zona de Seguridad y purga de datos.
 * 7. Pie de página con botón de cierre de sesión y diálogo de confirmación.
 *
 * @param state Estado inmutable de la pantalla ([SettingsUiState]).
 * @param onBackClick Acción al presionar el botón de volver atrás.
 * @param onEditProfileClick Acción para editar datos de perfil.
 * @param onDynamicBreaksToggle Acción para alternar pausas dinámicas.
 * @param onSilenceNotificationsToggle Acción para alternar silenciado de notificaciones.
 * @param onFocusTemplatesClick Acción para abrir el gestor de plantillas.
 * @param onPriorityAlertsToggle Acción para alternar alertas prioritarias P1.
 * @param onThemeSelectorClick Acción para abrir selector de tema.
 * @param onSecurityZoneClick Acción para navegar a la zona de seguridad.
 * @param onRequestLogout Acción para solicitar el cierre de sesión (abre el diálogo).
 * @param onDismissLogoutDialog Acción para cancelar el cierre de sesión y permanecer en KRONO.
 * @param onConfirmLogout Acción para confirmar el cierre de sesión.
 * @param modifier Modificador de diseño.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onNotificationPrefsClick: () -> Unit = {},
    onDynamicBreaksToggle: (Boolean) -> Unit,
    onSilenceNotificationsToggle: (Boolean) -> Unit,
    onFocusTemplatesClick: () -> Unit,
    onPriorityAlertsToggle: (Boolean) -> Unit,
    onThemeSelectorClick: () -> Unit,
    onSecurityZoneClick: () -> Unit = {},
    onRequestLogout: () -> Unit,
    onDismissLogoutDialog: () -> Unit,
    onConfirmLogout: () -> Unit,
    showFocusTemplatesDialog: Boolean = false,
    onDismissFocusTemplatesDialog: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
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
            // 1. Barra superior
            SettingsTopBar(onBackClick = onBackClick)

            // 2. Tarjeta de identidad de perfil (F-19)
            ProfileHeader(
                state = state,
                onEditProfileClick = onEditProfileClick,
            )

            // 3. Bloque: RITMO Y DESCANSO (Inteligencia Circadiana)
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KronoSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_section_rhythm),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryFixedDim,
                    )
                    Surface(
                        shape = RoundedCornerShape(KronoSpacing.xs),
                        color = SecondaryCyan.copy(alpha = 0.10f),
                        border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.25f)),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_badge_ai_sync),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            modifier = Modifier.padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.xs / 2),
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface(cornerRadius = KronoSpacing.md)
                        .padding(KronoSpacing.md),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_dynamic_breaks_title),
                            description = stringResource(R.string.settings_dynamic_breaks_desc),
                            checked = state.dynamicBreaksEnabled,
                            onCheckedChange = onDynamicBreaksToggle,
                        )
                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f), thickness = KronoGlass.borderWidth)
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_silence_notifs_title),
                            description = stringResource(R.string.settings_silence_notifs_desc),
                            checked = state.silenceInternalNotificationsEnabled,
                            onCheckedChange = onSilenceNotificationsToggle,
                        )
                    }
                }
            }

            // 4. Bloque: CONFIGURACIÓN DE SESIONES (F-10 / F-20)
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Text(
                    text = stringResource(R.string.settings_section_sessions),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = KronoSpacing.xs),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface(cornerRadius = KronoSpacing.md)
                        .clickable(role = Role.Button, onClick = onFocusTemplatesClick)
                        .padding(KronoSpacing.md),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_templates_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OnSurface,
                                )
                                Surface(
                                    shape = RoundedCornerShape(KronoSpacing.xs),
                                    color = SecondaryCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.3f)),
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_templates_presets, state.activePresetsCount),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SecondaryCyan,
                                        modifier = Modifier.padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.xs / 2),
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.settings_templates_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }

                        Icon(
                            painter = painterResource(KronoIcons.ArrowForward),
                            contentDescription = stringResource(R.string.settings_cd_open_templates),
                            tint = OnSurfaceVariant,
                            modifier = Modifier.size(KronoSizes.iconMedium),
                        )
                    }
                }
            }

            // 5. Bloque: PREFERENCIAS DEL SISTEMA
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Text(
                    text = stringResource(R.string.settings_section_system),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = KronoSpacing.xs),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassSurface(cornerRadius = KronoSpacing.md)
                        .padding(KronoSpacing.md),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
                        SettingsToggleRow(
                            title = stringResource(R.string.settings_priority_alerts_title),
                            description = stringResource(R.string.settings_priority_alerts_desc),
                            checked = state.priorityAlertsEnabled,
                            onCheckedChange = onPriorityAlertsToggle,
                        )

                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f), thickness = KronoGlass.borderWidth)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onNotificationPrefsClick)
                                .padding(vertical = KronoSpacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_notifications_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_notifications_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                )
                            }

                            Icon(
                                painter = painterResource(KronoIcons.ArrowForward),
                                contentDescription = stringResource(R.string.settings_cd_open_notifications),
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(KronoSizes.iconMedium),
                            )
                        }

                        HorizontalDivider(color = OutlineVariant.copy(alpha = 0.35f), thickness = KronoGlass.borderWidth)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button, onClick = onThemeSelectorClick)
                                .padding(vertical = KronoSpacing.xs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_screen_theme_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OnSurface,
                                )
                                Text(
                                    text = stringResource(R.string.settings_screen_theme_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(KronoSpacing.sm),
                                color = SurfaceContainerLow,
                                border = BorderStroke(KronoGlass.borderWidth, OutlineVariant.copy(alpha = 0.5f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(KronoSpacing.sm)
                                            .clip(CircleShape)
                                            .background(PrimaryMagenta),
                                    )
                                    Text(
                                        text = state.currentThemeName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface,
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = stringResource(R.string.settings_cd_theme_selector),
                                        tint = OnSurfaceVariant,
                                        modifier = Modifier.size(KronoSizes.iconSmall),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Bloque: Zona de Seguridad (F-21)
            SecurityZoneCard(onClick = onSecurityZoneClick)

            // 7. Pie de página y acción de cierre de sesión
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = KronoSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            ) {
                Text(
                    text = state.appVersionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )

                TextButton(
                    onClick = onRequestLogout,
                    modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.settings_logout_action),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = ErrorColor,
                    )
                }
            }
        }

        // Diálogo Oficial de Confirmación de Cierre de Sesión (F-25 CA-4)
        if (state.showLogoutConfirmationDialog) {
            LogoutConfirmationDialog(
                state = state,
                onDismissRequest = onDismissLogoutDialog,
                onConfirmLogout = onConfirmLogout,
            )
        }

        // Diálogo Informativo de Plantillas de Concentración (F-10 / F-20)
        if (showFocusTemplatesDialog) {
            FocusTemplatesDialog(
                onDismissRequest = onDismissFocusTemplatesDialog,
            )
        }
    }
}

// =========================================================================
// Previews de Compose con KronoTheme
// =========================================================================

/** Preview 1: Estado por defecto del Hub de Ajustes. */
@Preview(name = "Hub de Ajustes - Por Defecto", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun SettingsScreenDefaultPreview() {
    KronoTheme {
        SettingsScreen(
            state = SettingsUiState(
                userName = "Juan Esteban",
                userEmail = "juan.esteban@correo.com",
                userRole = "Estudiante y trabajador",
                dynamicBreaksEnabled = true,
                silenceInternalNotificationsEnabled = true,
                priorityAlertsEnabled = true,
                showLogoutConfirmationDialog = false,
            ),
            onBackClick = {},
            onEditProfileClick = {},
            onDynamicBreaksToggle = {},
            onSilenceNotificationsToggle = {},
            onFocusTemplatesClick = {},
            onPriorityAlertsToggle = {},
            onThemeSelectorClick = {},
            onSecurityZoneClick = {},
            onRequestLogout = {},
            onDismissLogoutDialog = {},
            onConfirmLogout = {},
        )
    }
}

/** Preview 2: Diálogo oficial de cierre de sesión abierto. */
@Preview(name = "Diálogo Cierre de Sesión Abierto", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun SettingsScreenLogoutDialogPreview() {
    KronoTheme {
        SettingsScreen(
            state = SettingsUiState(
                userName = "Juan Esteban",
                userEmail = "juan.esteban@correo.com",
                userRole = "Estudiante y trabajador",
                showLogoutConfirmationDialog = true,
                isLoggingOut = false,
            ),
            onBackClick = {},
            onEditProfileClick = {},
            onDynamicBreaksToggle = {},
            onSilenceNotificationsToggle = {},
            onFocusTemplatesClick = {},
            onPriorityAlertsToggle = {},
            onThemeSelectorClick = {},
            onSecurityZoneClick = {},
            onRequestLogout = {},
            onDismissLogoutDialog = {},
            onConfirmLogout = {},
        )
    }
}

/** Preview 3: Diálogo con operación de cierre de sesión en progreso. */
@Preview(name = "Cierre de Sesión en Progreso", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun SettingsScreenLoggingOutPreview() {
    KronoTheme {
        SettingsScreen(
            state = SettingsUiState(
                userName = "Juan Esteban",
                userEmail = "juan.esteban@correo.com",
                userRole = "Estudiante y trabajador",
                showLogoutConfirmationDialog = true,
                isLoggingOut = true,
            ),
            onBackClick = {},
            onEditProfileClick = {},
            onDynamicBreaksToggle = {},
            onSilenceNotificationsToggle = {},
            onFocusTemplatesClick = {},
            onPriorityAlertsToggle = {},
            onThemeSelectorClick = {},
            onSecurityZoneClick = {},
            onRequestLogout = {},
            onDismissLogoutDialog = {},
            onConfirmLogout = {},
        )
    }
}

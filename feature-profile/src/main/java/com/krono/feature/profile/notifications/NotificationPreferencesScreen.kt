package com.krono.feature.profile.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
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

/**
 * Punto de entrada con estado (Stateful) de la pantalla de Notificaciones y Alarmas (F-15 CA-1, CA-2, CA-3).
 *
 * Conecta [NotificationPreferencesViewModel] con la UI, gestiona la solicitud segura del permiso
 * del sistema [Manifest.permission.POST_NOTIFICATIONS] en Android 13+ (Tiramisu, CA-3), y escucha
 * los eventos de navegación y notificación.
 *
 * @param onBackClick Navegación hacia atrás (retorno al Hub de Ajustes).
 * @param viewModel ViewModel inyectado mediante Hilt.
 */
@Composable
fun NotificationPreferencesRoute(
    onBackClick: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Verificación inicial del permiso en tiempo de ejecución (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            viewModel.onPermissionResult(isGranted)
        } else {
            viewModel.onPermissionResult(true)
        }
    }

    // Launcher seguro de solicitud de permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Colector de eventos desacoplados de ViewModel
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                NotificationPreferencesEvent.NavigateBack -> onBackClick()
                NotificationPreferencesEvent.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onPermissionResult(true)
                    }
                }
                NotificationPreferencesEvent.PreferencesSaved -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.notification_prefs_success_saved),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onBackClick()
                }
                NotificationPreferencesEvent.DefaultsRestored -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.notification_prefs_defaults_restored),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    NotificationPreferencesScreen(
        state = state,
        onBackClick = viewModel::onBackClick,
        onDeadlineReminderMinutesChange = viewModel::onDeadlineReminderMinutesChange,
        onDeadlineRemindersEnabledChange = viewModel::onDeadlineRemindersEnabledChange,
        onFocusStartSoundChange = viewModel::onFocusStartSoundChange,
        onSoundToneChange = viewModel::onSoundToneChange,
        onRequestPermissionClick = viewModel::onRequestPermissionClick,
        onResetDefaultsClick = viewModel::onResetToDefaultsClick,
        onSaveClick = viewModel::onSaveClick,
    )
}

/**
 * Pantalla principal stateless de Preferencias de Notificaciones y Alarmas (F-15 CA-1, CA-2, CA-3).
 *
 * Diseñada estrictamente bajo el Lumina Glass System y especificación de Stitch:
 * 1. Cabecera con botón volver (48dp), placa de Ajustes, título y subtítulo explicativo.
 * 2. Tarjeta / Banner de estado de permiso del sistema con acción para activarlo (CA-3).
 * 3. Tarjeta central de Recordatorios de Tareas con selector de minutos (CA-1, CA-2) y sonido de enfoque.
 * 4. Botón de guardado con bloqueo concurrente y botón de restablecer valores de fábrica (CA-2).
 *
 * @param state Estado inmutable visual ([NotificationPreferencesUiState]).
 * @param onBackClick Acción al presionar el botón de regreso.
 * @param onDeadlineReminderMinutesChange Acción al cambiar los minutos de anticipación.
 * @param onDeadlineRemindersEnabledChange Acción al alternar avisos de entrega.
 * @param onFocusStartSoundChange Acción al alternar el tono de inicio de enfoque.
 * @param onSoundToneChange Acción al seleccionar un nuevo tono sonoro.
 * @param onRequestPermissionClick Acción para solicitar permiso de notificaciones en el sistema.
 * @param onResetDefaultsClick Acción para restablecer a 30 minutos y valores de fábrica (CA-2).
 * @param onSaveClick Acción para guardar las preferencias.
 * @param modifier Modificador de diseño.
 */
@Composable
fun NotificationPreferencesScreen(
    state: NotificationPreferencesUiState,
    onBackClick: () -> Unit,
    onDeadlineReminderMinutesChange: (Int) -> Unit,
    onDeadlineRemindersEnabledChange: (Boolean) -> Unit,
    onFocusStartSoundChange: (Boolean) -> Unit,
    onSoundToneChange: (String) -> Unit = {},
    onRequestPermissionClick: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showToneDialog by remember { mutableStateOf(false) }

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
            // 1. Barra superior y títulos
            NotificationPreferencesTopBar(onBackClick = onBackClick)

            // 2. Banner de permisos de notificación (Android 13+ CA-3)
            NotificationPermissionCard(
                isPermissionGranted = state.isPermissionGranted,
                onRequestPermissionClick = onRequestPermissionClick,
            )

            // 3. Bloque principal: Recordatorios de Tareas
            Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
                Text(
                    text = stringResource(R.string.notification_prefs_section_tasks),
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
                    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.md)) {
                        // Fila 1: Avisos de entrega
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(KronoSizes.iconLarge)
                                        .clip(RoundedCornerShape(KronoSpacing.sm))
                                        .background(SurfaceContainerHighest)
                                        .border(
                                            width = KronoGlass.borderWidth,
                                            color = OutlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(KronoSpacing.sm),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = stringResource(R.string.notification_prefs_cd_deadline_icon),
                                        tint = PrimaryMagenta,
                                        modifier = Modifier.size(KronoSizes.iconMedium),
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2)) {
                                    Text(
                                        text = stringResource(R.string.notification_prefs_deadline_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.notification_prefs_deadline_desc,
                                            state.deadlineReminderMinutes,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                    )
                                }
                            }

                            Switch(
                                checked = state.deadlineRemindersEnabled,
                                onCheckedChange = onDeadlineRemindersEnabledChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnSurface,
                                    checkedTrackColor = PrimaryMagenta,
                                    uncheckedThumbColor = OnSurfaceVariant,
                                    uncheckedTrackColor = SurfaceContainerHighest,
                                    uncheckedBorderColor = OutlineVariant,
                                ),
                            )
                        }

                        // Selector de minutos de anticipación (CA-1, CA-2)
                        AnimatedVisibility(
                            visible = state.deadlineRemindersEnabled,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = KronoSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                            ) {
                                Text(
                                    text = stringResource(R.string.notification_prefs_anticipation_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = OnSurfaceVariant,
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                                ) {
                                    NotificationPreferencesUiState.AVAILABLE_REMINDER_MINUTES.forEach { minutes ->
                                        val isSelected = minutes == state.deadlineReminderMinutes
                                        val chipText = when (minutes) {
                                            10 -> stringResource(R.string.notification_prefs_min_10)
                                            15 -> stringResource(R.string.notification_prefs_min_15)
                                            30 -> stringResource(R.string.notification_prefs_min_30)
                                            60 -> stringResource(R.string.notification_prefs_min_60)
                                            else -> "$minutes min"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(KronoSpacing.sm),
                                            color = if (isSelected) PrimaryMagenta.copy(alpha = 0.16f) else SurfaceContainerLow,
                                            border = BorderStroke(
                                                width = KronoGlass.borderWidth,
                                                color = if (isSelected) PrimaryMagenta else OutlineVariant.copy(alpha = 0.4f),
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = KronoSizes.minTouchTarget)
                                                .clickable(
                                                    role = Role.RadioButton,
                                                    onClick = { onDeadlineReminderMinutesChange(minutes) },
                                                ),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = KronoSpacing.xs, vertical = KronoSpacing.xs),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = chipText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) PrimaryFixedDim else OnSurfaceVariant,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = OutlineVariant.copy(alpha = 0.35f),
                            thickness = KronoGlass.borderWidth,
                        )

                        // Fila 2: Alarma sonora de inicio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(KronoSizes.iconLarge)
                                        .clip(RoundedCornerShape(KronoSpacing.sm))
                                        .background(SurfaceContainerHighest)
                                        .border(
                                            width = KronoGlass.borderWidth,
                                            color = OutlineVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(KronoSpacing.sm),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = stringResource(R.string.notification_prefs_cd_sound_icon),
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(KronoSizes.iconMedium),
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2)) {
                                    Text(
                                        text = stringResource(R.string.notification_prefs_sound_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OnSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.notification_prefs_sound_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnSurfaceVariant,
                                    )
                                }
                            }

                            Switch(
                                checked = state.focusStartSound,
                                onCheckedChange = onFocusStartSoundChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnSurface,
                                    checkedTrackColor = PrimaryMagenta,
                                    uncheckedThumbColor = OnSurfaceVariant,
                                    uncheckedTrackColor = SurfaceContainerHighest,
                                    uncheckedBorderColor = OutlineVariant,
                                ),
                            )
                        }

                        // Selector interactivo de tono sonoro (F-15)
                        AnimatedVisibility(
                            visible = state.focusStartSound,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = KronoSpacing.xs),
                                verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                            ) {
                                HorizontalDivider(
                                    color = OutlineVariant.copy(alpha = 0.25f),
                                    thickness = KronoGlass.borderWidth,
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(role = Role.Button, onClick = { showToneDialog = true })
                                        .padding(vertical = KronoSpacing.xs),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.notification_prefs_sound_tone_title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OnSurface,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.notification_prefs_sound_tone_desc,
                                                state.soundTone,
                                            ),
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
                                            Text(
                                                text = state.soundTone,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SecondaryCyan,
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = stringResource(R.string.notification_prefs_cd_tone_selector),
                                                tint = OnSurfaceVariant,
                                                modifier = Modifier.size(KronoSizes.iconSmall),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Mensaje de error si la persistencia falla
            if (state.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(KronoSpacing.sm),
                    color = ErrorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorColor,
                        modifier = Modifier.padding(KronoSpacing.md),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            // 4. Botones de acción inferiores
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = KronoSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = onSaveClick,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.controlHeight),
                    shape = RoundedCornerShape(KronoSpacing.sm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryMagenta,
                        contentColor = OnSurface,
                        disabledContainerColor = PrimaryMagenta.copy(alpha = 0.5f),
                        disabledContentColor = OnSurface.copy(alpha = 0.6f),
                    ),
                    contentPadding = PaddingValues(horizontal = KronoSpacing.lg, vertical = KronoSpacing.md),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(KronoSizes.iconSmall),
                            color = OnSurface,
                            strokeWidth = KronoSizes.progressStroke,
                        )
                        Spacer(modifier = Modifier.size(KronoSpacing.sm))
                        Text(
                            text = stringResource(R.string.notification_prefs_action_saving),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(KronoSizes.iconMedium),
                        )
                        Spacer(modifier = Modifier.size(KronoSpacing.sm))
                        Text(
                            text = stringResource(R.string.notification_prefs_action_save),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                TextButton(
                    onClick = onResetDefaultsClick,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.minTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.notification_prefs_action_reset),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant,
                    )
                }
            }
        }

        if (showToneDialog) {
            ToneSelectionDialog(
                selectedTone = state.soundTone,
                onToneSelected = onSoundToneChange,
                onDismissRequest = { showToneDialog = false },
            )
        }
    }
}

/**
 * Diálogo modal para seleccionar el tono sonoro de las alarmas de concentración (F-15).
 */
@Composable
private fun ToneSelectionDialog(
    selectedTone: String,
    onToneSelected: (String) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
            ) {
                Text(
                    text = stringResource(R.string.notification_prefs_dialog_tone_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                )

                val tones = listOf(
                    "Chime" to stringResource(R.string.notification_prefs_tone_chime),
                    "Pulse" to stringResource(R.string.notification_prefs_tone_pulse),
                    "Campana" to stringResource(R.string.notification_prefs_tone_campana),
                    "Silencioso" to stringResource(R.string.notification_prefs_tone_silencioso),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                ) {
                    tones.forEach { (toneId, toneLabel) ->
                        val isSelected = toneId.equals(selectedTone, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(KronoSpacing.sm),
                            color = if (isSelected) PrimaryMagenta.copy(alpha = 0.15f) else SurfaceContainerLow,
                            border = BorderStroke(
                                width = KronoGlass.borderWidth,
                                color = if (isSelected) PrimaryMagenta else OutlineVariant.copy(alpha = 0.4f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = KronoSizes.minTouchTarget)
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = {
                                        onToneSelected(toneId)
                                        onDismissRequest()
                                    },
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = toneLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryFixedDim else OnSurface,
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryFixedDim,
                                        modifier = Modifier.size(KronoSizes.iconSmall),
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = KronoSizes.minTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.security_dialog_delete_cancel),
                        color = OnSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Cabecera superior con botón para volver al perfil y badge de Ajustes.
 */
@Composable
private fun NotificationPreferencesTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = SurfaceContainerLow,
                border = BorderStroke(KronoGlass.borderWidth, OutlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .heightIn(min = KronoSizes.minTouchTarget)
                    .clickable(
                        role = Role.Button,
                        onClick = onBackClick,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
                ) {
                    Icon(
                        painter = painterResource(KronoIcons.ArrowBack),
                        contentDescription = stringResource(R.string.notification_prefs_cd_back),
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(KronoSizes.iconSmall),
                    )
                    Text(
                        text = stringResource(R.string.notification_prefs_back),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(R.string.notification_prefs_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs / 2)) {
            Text(
                text = stringResource(R.string.notification_prefs_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
            )
            Text(
                text = stringResource(R.string.notification_prefs_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
        }
    }
}

/**
 * Tarjeta indicadora de estado y solicitud del permiso de notificaciones (CA-3).
 */
@Composable
private fun NotificationPermissionCard(
    isPermissionGranted: Boolean,
    onRequestPermissionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isPermissionGranted) {
        Surface(
            shape = RoundedCornerShape(KronoSpacing.sm),
            color = SecondaryCyan.copy(alpha = 0.08f),
            border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = 0.3f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            ) {
                Icon(
                    painter = painterResource(KronoIcons.CheckCircle),
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Text(
                    text = stringResource(R.string.notification_prefs_permission_granted_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryCyan,
                )
            }
        }
    } else {
        Surface(
            shape = RoundedCornerShape(KronoSpacing.md),
            color = SurfaceContainerHigh.copy(alpha = 0.7f),
            border = BorderStroke(KronoGlass.borderWidth, PrimaryMagenta.copy(alpha = 0.5f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(KronoSpacing.md),
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.notification_prefs_cd_permission_icon),
                        tint = PrimaryFixedDim,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                    Text(
                        text = stringResource(R.string.notification_prefs_permission_banner_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnSurface,
                    )
                }

                Text(
                    text = stringResource(R.string.notification_prefs_permission_banner_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )

                Button(
                    onClick = onRequestPermissionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = KronoSizes.minTouchTarget),
                    shape = RoundedCornerShape(KronoSpacing.sm),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryMagenta.copy(alpha = 0.25f),
                        contentColor = PrimaryFixedDim,
                    ),
                    border = BorderStroke(KronoGlass.borderWidth, PrimaryMagenta.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = stringResource(R.string.notification_prefs_permission_action_grant),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Preview(name = "Notificaciones y Alarmas - Permiso Concedido", showBackground = true)
@Composable
private fun NotificationPreferencesScreenPreview() {
    KronoTheme {
        NotificationPreferencesScreen(
            state = NotificationPreferencesUiState(
                deadlineReminderMinutes = 30,
                deadlineRemindersEnabled = true,
                focusStartSound = true,
                isPermissionGranted = true,
            ),
            onBackClick = {},
            onDeadlineReminderMinutesChange = {},
            onDeadlineRemindersEnabledChange = {},
            onFocusStartSoundChange = {},
            onRequestPermissionClick = {},
            onResetDefaultsClick = {},
            onSaveClick = {},
        )
    }
}

@Preview(name = "Notificaciones y Alarmas - Permiso Pendiente", showBackground = true)
@Composable
private fun NotificationPreferencesScreenPermissionPendingPreview() {
    KronoTheme {
        NotificationPreferencesScreen(
            state = NotificationPreferencesUiState(
                deadlineReminderMinutes = 15,
                deadlineRemindersEnabled = true,
                focusStartSound = false,
                isPermissionGranted = false,
            ),
            onBackClick = {},
            onDeadlineReminderMinutesChange = {},
            onDeadlineRemindersEnabledChange = {},
            onFocusStartSoundChange = {},
            onRequestPermissionClick = {},
            onResetDefaultsClick = {},
            onSaveClick = {},
        )
    }
}

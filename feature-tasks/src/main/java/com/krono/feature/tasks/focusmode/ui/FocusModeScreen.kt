package com.krono.feature.tasks.focusmode.ui

import androidx.annotation.StringRes
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.timer.FocusPhase
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focusmode.FocusModeUiState
import com.krono.feature.tasks.focusmode.FocusModeViewModel
import com.krono.feature.tasks.timer.ui.ClockRing
import com.krono.feature.tasks.timer.ui.formatClock
import com.krono.feature.tasks.timer.ui.formatCountdown
import kotlin.time.Duration

/**
 * Punto de entrada con estado del modo enfoque de una tarea (F-09).
 *
 * @param onBack vuelve a la pantalla anterior; el enfoque sigue corriendo y se puede retomar.
 */
@Composable
fun FocusModeRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = state.errorMessageRes?.let { stringResource(it) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    FocusModeScreen(
        state = state,
        actions = FocusModeActions(
            onBack = onBack,
            onPause = viewModel::onPause,
            onResume = viewModel::onResume,
            onExtraBreak = viewModel::onExtraBreak,
            onTakeTemplateBreak = viewModel::onTakeTemplateBreak,
            onFinish = viewModel::onFinish,
            onConfirmSwitch = viewModel::onConfirmSwitch,
            onDismissSwitch = {
                viewModel.onDismissSwitch()
                onBack()
            },
        ),
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Acciones que la pantalla del modo enfoque delega en su ViewModel. */
data class FocusModeActions(
    val onBack: () -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onExtraBreak: () -> Unit = {},
    val onTakeTemplateBreak: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onConfirmSwitch: () -> Unit = {},
    val onDismissSwitch: () -> Unit = {},
)

/**
 * Pantalla del modo enfoque (F-09 CA-1 a CA-3), con la estética del diseño `modo_enfoque_krono_1`:
 * tarjeta de la tarea, anillo con la cuenta regresiva y controles. Al llegar a cero o al terminar
 * un descanso avisa dentro de la app y con una vibración corta; no depende de permisos.
 */
@Composable
fun FocusModeScreen(
    state: FocusModeUiState,
    actions: FocusModeActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    AlertHaptics(state.phase)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DeepMidnight,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = KronoSizes.formMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(KronoSpacing.lg),
            ) {
                BackRow(actions.onBack)
                when {
                    state.isLoading -> CircularProgressIndicator(color = PrimaryFixedDim)
                    state.task == null -> NotFoundMessage()
                    else -> FocusContent(state, actions)
                }
            }
        }
    }
    FocusDialogs(state, actions)
}

/** Vibración corta cuando hay que llamar la atención del usuario: bloque en cero o descanso terminado. */
@Composable
private fun AlertHaptics(phase: FocusPhase?) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(phase) {
        if (phase == FocusPhase.BLOCK_COMPLETE || phase == FocusPhase.BREAK_OVER) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

@Composable
private fun FocusContent(state: FocusModeUiState, actions: FocusModeActions) {
    val phase = state.phase
    if (phase != null) StatusChip(phase)
    FocusTaskCard(state)
    if (state.isTaskDeleted) Notice(R.string.focus_mode_task_deleted)
    if (phase == null) {
        if (state.savedSession == null && !state.isSessionTooShort) {
            Notice(R.string.focus_mode_idle)
        } else {
            SessionOutcomeMessage(state.savedSession, state.isSavedSessionWithoutTask, state.isSessionTooShort)
        }
        return
    }
    FocusRing(state, phase)
    if (phase == FocusPhase.BREAK_OVER) Notice(R.string.focus_mode_break_over)
    FocusModeControls(
        phase = phase,
        enabled = !state.isBusy,
        onPause = actions.onPause,
        onResume = actions.onResume,
        onExtraBreak = actions.onExtraBreak,
        onFinish = actions.onFinish,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.focus_mode_background_hint),
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Anillo con la cuenta regresiva del bloque o, durante un descanso, la del descanso. */
@Composable
private fun FocusRing(state: FocusModeUiState, phase: FocusPhase) {
    val onBreak = phase == FocusPhase.ON_BREAK
    val shown = if (onBreak) state.breakRemaining ?: state.remaining else state.remaining
    val total = if (onBreak) state.breakDuration ?: state.blockDuration else state.blockDuration
    val clock = formatCountdown(shown)
    val progress = if (total.isPositive()) (shown / total).toFloat().coerceIn(0f, 1f) else 0f
    val descriptionRes = if (onBreak) R.string.cd_focus_mode_break_remaining else R.string.cd_focus_mode_remaining
    val captionRes = if (onBreak) R.string.focus_mode_caption_break else R.string.focus_mode_caption_remaining
    ClockRing(
        clock = clock,
        progress = progress,
        caption = stringResource(captionRes),
        description = stringResource(descriptionRes, clock),
    )
}

@Composable
private fun FocusDialogs(state: FocusModeUiState, actions: FocusModeActions) {
    if (state.phase == FocusPhase.BLOCK_COMPLETE) {
        BlockCompleteDialog(
            breakMinutes = state.templateBreak.inWholeMinutes.toInt(),
            enabled = !state.isBusy,
            onTakeBreak = actions.onTakeTemplateBreak,
            onFinish = actions.onFinish,
        )
    }
    if (state.isConfirmingSwitch) {
        SwitchSessionDialog(onConfirm = actions.onConfirmSwitch, onDismiss = actions.onDismissSwitch)
    }
}

@Composable
private fun BackRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onBack),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(KronoIcons.ArrowBack),
            contentDescription = null,
            tint = OnSurface,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
        Text(
            text = stringResource(R.string.timer_back),
            style = MaterialTheme.typography.labelLarge,
            color = OnSurface,
        )
    }
}

@Composable
private fun StatusChip(phase: FocusPhase) {
    val label = when (phase) {
        FocusPhase.WORKING -> R.string.focus_mode_chip_working
        FocusPhase.PAUSED -> R.string.focus_mode_chip_paused
        FocusPhase.ON_BREAK -> R.string.focus_mode_chip_break
        FocusPhase.BREAK_OVER -> R.string.focus_mode_chip_break_over
        FocusPhase.BLOCK_COMPLETE -> R.string.focus_mode_chip_block_complete
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(SurfaceContainerHigh)
            .padding(horizontal = KronoSpacing.md, vertical = KronoSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KronoSpacing.sm)
                .clip(CircleShape)
                .background(if (phase == FocusPhase.WORKING) SecondaryCyan else OnSurfaceVariant),
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = SecondaryCyan,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Aviso en una tarjeta de vidrio; se anuncia a los lectores de pantalla cuando aparece. */
@Composable
private fun Notice(@StringRes messageRes: Int) {
    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.bodyLarge,
        color = SecondaryCyan,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * Resultado de finalizar el enfoque: confirma la sesión guardada, avisa si el tiempo se guardó sin sumarse a
 * ninguna tarea (ya no existe) o si la sesión fue demasiado corta y se descartó. No muestra nada
 * si no hay resultado que informar.
 */
@Composable
private fun SessionOutcomeMessage(savedSession: Duration?, withoutTask: Boolean, tooShort: Boolean) {
    val text = when {
        tooShort -> stringResource(R.string.timer_too_short)
        savedSession == null -> return
        withoutTask -> stringResource(R.string.timer_saved_without_task, formatClock(savedSession))
        else -> stringResource(R.string.timer_saved, formatClock(savedSession))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = SecondaryCyan,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun NotFoundMessage() {
    Text(
        text = stringResource(R.string.timer_task_not_found),
        style = MaterialTheme.typography.titleMedium,
        color = OnSurface,
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.lg),
        textAlign = TextAlign.Center,
    )
}

/**
 * Aviso de que el bloque llegó a cero (CA-3): ofrece el descanso de la plantilla o finalizar. No se
 * cierra al tocar fuera, para que la decisión no se pierda sin querer.
 */
@Composable
private fun BlockCompleteDialog(
    breakMinutes: Int,
    enabled: Boolean,
    onTakeBreak: () -> Unit,
    onFinish: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = SurfaceContainerHigh,
        title = { Text(text = stringResource(R.string.focus_mode_complete_title), color = OnSurface) },
        text = {
            Text(text = stringResource(R.string.focus_mode_complete_body, breakMinutes), color = OnSurfaceVariant)
        },
        confirmButton = {
            TextButton(onClick = onTakeBreak, enabled = enabled, modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget)) {
                Text(text = stringResource(R.string.focus_mode_complete_break, breakMinutes), color = PrimaryFixedDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onFinish, enabled = enabled, modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget)) {
                Text(text = stringResource(R.string.focus_mode_complete_finish), color = OnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun SwitchSessionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = { Text(text = stringResource(R.string.focus_mode_switch_title), color = OnSurface) },
        text = { Text(text = stringResource(R.string.focus_mode_switch_body), color = OnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget)) {
                Text(text = stringResource(R.string.focus_mode_switch_confirm), color = PrimaryFixedDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget)) {
                Text(text = stringResource(R.string.focus_mode_switch_cancel), color = OnSurfaceVariant)
            }
        },
    )
}

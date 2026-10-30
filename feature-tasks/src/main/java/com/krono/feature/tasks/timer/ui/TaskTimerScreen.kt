package com.krono.feature.tasks.timer.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.task.Task
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
import com.krono.feature.tasks.timer.TaskTimerUiState
import com.krono.feature.tasks.timer.TaskTimerViewModel
import com.krono.feature.tasks.timer.TimerStatus
import com.krono.feature.tasks.ui.TaskDateFormatter
import kotlin.time.Duration

/**
 * Punto de entrada con estado del cronómetro de una tarea (F-08).
 *
 * @param onBack vuelve a la pantalla anterior; el cronómetro sigue corriendo (CA-3).
 */
@Composable
fun TaskTimerRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TaskTimerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorShown()
        }
    }

    TaskTimerScreen(
        state = state,
        onBack = onBack,
        onStart = viewModel::onStart,
        onPause = viewModel::onPause,
        onResume = viewModel::onResume,
        onFinish = viewModel::onFinish,
        onConfirmSwitch = viewModel::onConfirmSwitch,
        onDismissSwitch = viewModel::onDismissSwitch,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/**
 * Pantalla del cronómetro (F-08 CA-1 a CA-4), con la estética del diseño
 * `modo_enfoque_krono_1`: tarjeta de la tarea, anillo con el tiempo y controles.
 */
@Composable
fun TaskTimerScreen(
    state: TaskTimerUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onConfirmSwitch: () -> Unit,
    onDismissSwitch: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
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
                BackRow(onBack)
                when {
                    state.isLoading -> CircularProgressIndicator(color = PrimaryFixedDim)
                    state.task == null -> {
                        NotFoundMessage()
                        // La tarea pudo borrarse justo al finalizar: el aviso de la sesión sigue visible.
                        state.savedSession?.let { SavedSessionMessage(it, state.isSavedSessionWithoutTask) }
                    }
                    else -> TimerContent(state, state.task, onStart, onPause, onResume, onFinish)
                }
            }
        }
    }

    if (state.isConfirmingSwitch) {
        SwitchTimerDialog(onConfirm = onConfirmSwitch, onDismiss = onDismissSwitch)
    }
}

@Composable
private fun TimerContent(
    state: TaskTimerUiState,
    task: Task,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    StatusChip(state.status)
    TaskCard(task)
    TimerRing(elapsed = state.elapsed, estimateProgress = state.estimateProgress)
    TimerControls(
        status = state.status,
        enabled = !state.isBusy,
        onStart = onStart,
        onPause = onPause,
        onResume = onResume,
        onFinish = onFinish,
        modifier = Modifier.fillMaxWidth(),
    )
    state.savedSession?.let { SavedSessionMessage(it, state.isSavedSessionWithoutTask) }
    Text(
        text = stringResource(R.string.timer_background_hint),
        style = MaterialTheme.typography.bodySmall,
        color = OnSurfaceVariant,
        textAlign = TextAlign.Center,
    )
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
private fun StatusChip(status: TimerStatus) {
    val label = when (status) {
        TimerStatus.IDLE -> R.string.timer_chip_idle
        TimerStatus.RUNNING -> R.string.timer_chip_running
        TimerStatus.PAUSED -> R.string.timer_chip_paused
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
                .background(if (status == TimerStatus.RUNNING) SecondaryCyan else OnSurfaceVariant),
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
            color = SecondaryCyan,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun TaskCard(task: Task) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
        TaskDateFormatter.formatEstimatedDuration(context, task.estimatedDuration)?.let { estimate ->
            Text(
                text = stringResource(R.string.timer_estimate, estimate),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.timer_actual_total, formatClock(task.actualDuration)),
            style = MaterialTheme.typography.bodyMedium,
            color = PrimaryFixedDim,
        )
    }
}

/**
 * Confirma la sesión guardada (CA-2); si la tarea ya no existía ([withoutTask]), avisa que el
 * tiempo se guardó sin sumarse a ninguna tarea.
 */
@Composable
private fun SavedSessionMessage(duration: Duration, withoutTask: Boolean) {
    val messageRes = if (withoutTask) R.string.timer_saved_without_task else R.string.timer_saved
    Text(
        text = stringResource(messageRes, formatClock(duration)),
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

@Composable
private fun SwitchTimerDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(text = stringResource(R.string.timer_switch_title), color = OnSurface)
        },
        text = {
            Text(text = stringResource(R.string.timer_switch_body), color = OnSurfaceVariant)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.timer_switch_confirm), color = PrimaryFixedDim)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.timer_switch_cancel), color = OnSurfaceVariant)
            }
        },
    )
}

package com.krono.feature.tasks.assistant.agent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.assistant.agent.AskPlanningAgentUseCase
import com.krono.core.domain.assistant.agent.BatchSaveResult
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.assistant.agent.AgentFailure
import com.krono.feature.tasks.assistant.agent.AgentStatus
import com.krono.feature.tasks.assistant.agent.PlanningAgentUiState
import com.krono.feature.tasks.assistant.agent.PlanningAgentViewModel

/** Líneas visibles del campo del mensaje antes de desplazarse. */
private const val INPUT_MAX_LINES = 4

/** Punto de entrada con estado de la sección del agente de planificación (F-28). */
@Composable
fun PlanningAgentRoute(
    modifier: Modifier = Modifier,
    viewModel: PlanningAgentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PlanningAgentSection(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onConfirm = viewModel::onConfirm,
        onDiscard = viewModel::onDiscard,
        onRetryFailed = viewModel::retryFailedTasks,
        modifier = modifier,
    )
}

/**
 * Sección "Pídele algo al agente" del asistente de prioridades (F-28): campo de mensaje y,
 * según el paso, el progreso, la propuesta para confirmar o el resultado.
 */
@Composable
fun PlanningAgentSection(
    state: PlanningAgentUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onRetryFailed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        AgentHeader()
        AgentInput(state = state, onInputChange = onInputChange, onSend = onSend)
        Column(
            // Anuncia el progreso y la respuesta del agente a los lectores de pantalla.
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        ) {
            AgentStatusContent(
                status = state.status,
                onConfirm = onConfirm,
                onDiscard = onDiscard,
                onRetryFailed = onRetryFailed,
            )
        }
    }
}

@Composable
private fun AgentHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint = SecondaryCyan,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.agent_title),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = stringResource(R.string.agent_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun AgentInput(state: PlanningAgentUiState, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.agent_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Outline,
                )
            },
            maxLines = INPUT_MAX_LINES,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = KronoShapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
                focusedContainerColor = SurfaceContainerLow,
                unfocusedContainerColor = SurfaceContainerLow,
                cursorColor = PrimaryFixedDim,
                focusedBorderColor = PrimaryFixedDim,
                unfocusedBorderColor = OutlineVariant,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AgentPrimaryButton(
                text = stringResource(R.string.agent_send),
                enabled = state.canSend,
                onClick = onSend,
            )
        }
    }
}

@Composable
private fun AgentStatusContent(
    status: AgentStatus,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onRetryFailed: () -> Unit = {},
) {
    when (status) {
        AgentStatus.Idle -> Unit
        AgentStatus.Thinking -> ProgressLine(text = stringResource(R.string.agent_thinking))
        is AgentStatus.Proposed -> {
            ProposalContent(proposal = status.proposal)
            if (status.saveFailed) ErrorLine(text = stringResource(R.string.agent_save_failed))
            ProposalActions(draftCount = status.proposal.drafts.size, onConfirm = onConfirm, onDiscard = onDiscard)
        }
        is AgentStatus.Saving -> {
            status.proposal?.let { ProposalContent(proposal = it) }
            ProgressLine(text = stringResource(R.string.agent_saving))
        }
        is AgentStatus.Saved -> {
            val result = status.result
            if (result.failedTasks.isNotEmpty() || result.pendingSyncTasks.isNotEmpty()) {
                BatchSaveSummary(
                    result = result,
                    onRetryFailed = onRetryFailed,
                    onClose = onDiscard,
                )
            } else {
                ResultLine(
                    text = pluralStringResource(R.plurals.agent_saved, status.count, status.count),
                    color = SecondaryCyan,
                    onClose = onDiscard,
                )
            }
        }
        is AgentStatus.Failed -> ResultLine(text = failureText(status.reason), color = ErrorColor, onClose = onDiscard)
    }
}

@Composable
private fun BatchSaveSummary(
    result: BatchSaveResult,
    onRetryFailed: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.agent_batch_summary_title),
                style = MaterialTheme.typography.titleSmall,
                color = OnSurface,
                modifier = Modifier.semantics { heading() },
            )
            TextButton(onClick = onClose) {
                Text(text = stringResource(R.string.agent_close), color = OnSurfaceVariant)
            }
        }

        if (result.confirmedTasks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.krono.core.ui.R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = SecondaryCyan,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Text(
                    text = stringResource(R.string.agent_batch_confirmed, result.confirmedTasks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryCyan,
                )
            }
        }

        if (result.pendingSyncTasks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.krono.core.ui.R.drawable.ic_sync),
                    contentDescription = null,
                    tint = PrimaryFixedDim,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Text(
                    text = stringResource(R.string.agent_batch_pending_sync, result.pendingSyncTasks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryFixedDim,
                )
            }
        }

        if (result.failedTasks.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(com.krono.core.ui.R.drawable.ic_error),
                    contentDescription = null,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Text(
                    text = stringResource(R.string.agent_batch_failed, result.failedTasks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorColor,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                val retryCd = stringResource(R.string.agent_retry_failed_cd, result.failedTasks.size)
                AgentPrimaryButton(
                    text = stringResource(R.string.agent_retry_failed),
                    enabled = true,
                    onClick = onRetryFailed,
                    modifier = Modifier.semantics {
                        contentDescription = retryCd
                    },
                )
            }
        }
    }
}

@Composable
private fun ProposalActions(draftCount: Int, onConfirm: () -> Unit, onDiscard: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDiscard) {
            Text(text = stringResource(R.string.agent_discard), color = OnSurfaceVariant)
        }
        // Sin tareas propuestas (el agente solo pidió un dato) no hay nada que confirmar.
        if (draftCount > 0) {
            AgentPrimaryButton(
                text = pluralStringResource(R.plurals.agent_confirm, draftCount, draftCount),
                enabled = true,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun ProgressLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(KronoSizes.iconSmall),
            color = SecondaryCyan,
            strokeWidth = KronoSizes.progressStroke,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
    }
}

@Composable
private fun ErrorLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = ErrorColor)
}

@Composable
private fun ResultLine(text: String, color: Color, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClose) {
            Text(text = stringResource(R.string.agent_close), color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun failureText(reason: AgentFailure): String = when (reason) {
    AgentFailure.UNAVAILABLE -> stringResource(R.string.agent_error_unavailable)
    AgentFailure.NOT_UNDERSTOOD -> stringResource(R.string.agent_error_not_understood)
    AgentFailure.INVALID_REQUEST ->
        stringResource(R.string.agent_error_invalid, AskPlanningAgentUseCase.MAX_REQUEST_LENGTH)
}

package com.krono.feature.tasks.assistant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.krono.core.domain.assistant.PriorityPlan
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.assistant.PriorityAssistantUiState
import com.krono.feature.tasks.assistant.PriorityAssistantViewModel

/**
 * Punto de entrada con estado del asistente de prioridades (F-12).
 *
 * @param onBack vuelve a la pantalla anterior.
 * @param onOpenTask abre el detalle de una tarea del plan.
 */
@Composable
fun PriorityAssistantRoute(
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PriorityAssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    PriorityAssistantScreen(
        state = state,
        onBack = onBack,
        onOpenTask = onOpenTask,
        onRefresh = viewModel::onRefresh,
        modifier = modifier,
    )
}

/**
 * Pantalla del asistente de prioridades (F-12 CA-1, diseño `asistente_de_prioridades_ia_1`):
 * tarea sugerida con su motivo, secuencia para después y balance por categoría.
 */
@Composable
fun PriorityAssistantScreen(
    state: PriorityAssistantUiState,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = KronoSpacing.lg, vertical = KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        item { AssistantTopRow(onBack = onBack, onRefresh = onRefresh, canRefresh = state.plan != null) }
        item { AssistantHeader() }

        when {
            state.isLoading -> item { CenteredProgress() }
            state.hasError -> item { ErrorMessage(onRetry = onRefresh) }
            state.isEmpty -> item { EmptyMessage() }
            else -> state.plan?.let { plan -> planItems(plan, state, onOpenTask) }
        }
    }
}

private fun LazyListScope.planItems(
    plan: PriorityPlan,
    state: PriorityAssistantUiState,
    onOpenTask: (String) -> Unit,
) {
    item {
        RecommendationCard(
            recommended = plan.recommended,
            confidencePercent = plan.confidencePercent,
            explanation = state.explanation,
            onOpenTask = onOpenTask,
        )
    }
    if (plan.sequence.isNotEmpty()) {
        item { SequenceHeader(remaining = plan.sequence.size) }
        itemsIndexed(plan.sequence, key = { _, planned -> planned.task.id }) { index, planned ->
            SequenceItem(position = index + 1, planned = planned, onOpenTask = onOpenTask)
        }
    }
    item { BalanceCard(balance = plan.categoryBalance) }
}

@Composable
private fun AssistantTopRow(onBack: () -> Unit, onRefresh: () -> Unit, canRefresh: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = KronoSizes.minTouchTarget)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(end = KronoSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(KronoIcons.ArrowBack),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
            Text(
                text = stringResource(R.string.assistant_back),
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryFixedDim,
            )
        }
        if (canRefresh) {
            TextButton(onClick = onRefresh) {
                Text(text = stringResource(R.string.assistant_refresh), color = SecondaryCyan)
            }
        } else {
            AssistantBadge(text = stringResource(R.string.assistant_mode_chip), tint = SecondaryCyan)
        }
    }
}

@Composable
private fun AssistantHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
            Text(
                text = stringResource(R.string.assistant_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(
            text = stringResource(R.string.assistant_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(KronoSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = PrimaryFixedDim, strokeWidth = KronoSizes.progressStroke)
    }
}

@Composable
private fun EmptyMessage() {
    MessageCard(
        title = stringResource(R.string.assistant_empty_title),
        body = stringResource(R.string.assistant_empty_body),
    )
}

@Composable
private fun ErrorMessage(onRetry: () -> Unit) {
    MessageCard(title = stringResource(R.string.assistant_error)) {
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.assistant_retry), color = SecondaryCyan)
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    body: String? = null,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )
        body?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant, textAlign = TextAlign.Center)
        }
        action()
    }
}

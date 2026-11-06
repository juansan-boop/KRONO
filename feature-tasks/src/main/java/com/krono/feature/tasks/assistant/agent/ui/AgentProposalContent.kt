package com.krono.feature.tasks.assistant.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.assistant.agent.AgentProposal
import com.krono.core.domain.assistant.agent.TaskDraft
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.feature.tasks.R
import com.krono.feature.tasks.assistant.ui.AssistantBadge
import com.krono.feature.tasks.assistant.ui.assistantLabelRes
import com.krono.feature.tasks.assistant.ui.clockText
import com.krono.feature.tasks.assistant.ui.durationText
import com.krono.feature.tasks.assistant.ui.labelRes

/** Líneas máximas del título de una tarea en la propuesta antes de cortarlo con "…". */
private const val PROPOSAL_TITLE_MAX_LINES = 2

/**
 * Propuesta del agente (F-28): el mensaje de Gemini, las tareas que crearía con su
 * estimación H1 y el plan H2 que quedaría, con las tareas nuevas marcadas.
 */
@Composable
internal fun ProposalContent(proposal: AgentProposal) {
    proposal.message?.let { AgentMessage(it) }
    if (proposal.drafts.isNotEmpty()) {
        SectionTitle(stringResource(R.string.agent_drafts_title))
        proposal.drafts.forEach { DraftItem(it) }
    }
    val plan = proposal.plan
    if (plan != null && proposal.drafts.isNotEmpty()) {
        SectionTitle(stringResource(R.string.agent_plan_title))
        (listOf(plan.recommended) + plan.sequence).forEachIndexed { index, planned ->
            PlanItem(position = index + 1, planned = planned, isNew = planned.task.id in proposal.newTaskIds)
        }
        Text(
            text = stringResource(R.string.agent_plan_caption),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
        )
    }
}

/** Botón principal de la sección del agente, con el estilo de los botones del asistente. */
@Composable
internal fun AgentPrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
        shape = KronoShapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMagenta, contentColor = OnSurface),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AgentMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KronoShapes.small)
            .background(SurfaceContainerLow)
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
        Text(
            text = stringResource(R.string.agent_ai_caption),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = OnSurface)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DraftItem(draft: TaskDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        Text(
            text = draft.request.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = OnSurface,
            maxLines = PROPOSAL_TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            AssistantBadge(text = stringResource(draft.request.category.labelRes()), tint = SecondaryCyan)
            AssistantBadge(
                text = stringResource(draft.request.priority.assistantLabelRes()),
                tint = PrimaryFixedDim,
                iconRes = R.drawable.ic_flag,
            )
            AssistantBadge(text = dueText(draft), tint = OnSurfaceVariant)
            draft.estimate?.let {
                AssistantBadge(
                    text = stringResource(
                        R.string.assistant_duration_with_source,
                        durationText(it.duration),
                        stringResource(it.source.labelRes()),
                    ),
                    tint = SecondaryCyan,
                    iconRes = R.drawable.ic_schedule,
                )
            }
        }
    }
}

@Composable
private fun PlanItem(position: Int, planned: PlannedTask, isNew: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = clockText(planned.start),
            style = MaterialTheme.typography.labelLarge,
            color = SecondaryCyan,
        )
        Text(
            text = stringResource(R.string.agent_plan_item, position, planned.task.title),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurface,
            maxLines = PROPOSAL_TITLE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isNew) AssistantBadge(text = stringResource(R.string.agent_new_badge), tint = PrimaryFixedDim)
        AssistantBadge(text = durationText(planned.estimate), tint = OnSurfaceVariant)
    }
}

@Composable
private fun dueText(draft: TaskDraft): String {
    val due = draft.request.dueDate ?: return stringResource(R.string.agent_draft_no_due)
    return stringResource(R.string.agent_draft_due, due.dayOfMonth, due.monthNumber, due.hour, due.minute)
}

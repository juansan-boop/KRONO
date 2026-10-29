package com.krono.feature.tasks.assistant.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.krono.core.domain.assistant.PlannedTask
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.assistant.ExplanationState

/**
 * Tarjeta principal del asistente: la tarea sugerida para ahora, su motivo y el bloque de
 * tiempo propuesto (F-12 CA-1, diseño `asistente_de_prioridades_ia_1`).
 *
 * @param recommended tarea recomendada.
 * @param confidencePercent confianza de la heurística en la recomendación.
 * @param explanation estado de la explicación redactada por la IA.
 * @param onOpenTask abre el detalle de la tarea recomendada.
 */
@Composable
internal fun RecommendationCard(
    recommended: PlannedTask,
    confidencePercent: Int,
    explanation: ExplanationState,
    onOpenTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        SuggestionHeader(confidencePercent)
        TaskSummary(recommended)
        ExplanationBox(recommended, explanation)
        TimeBlockBox(recommended)
        OpenTaskButton(onClick = { onOpenTask(recommended.task.id) })
    }
}

@Composable
private fun SuggestionHeader(confidencePercent: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.assistant_suggestion_label),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryFixedDim,
            )
        }
        AssistantBadge(
            text = stringResource(R.string.assistant_confidence, confidencePercent),
            tint = PrimaryFixedDim,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskSummary(recommended: PlannedTask) {
    Column(verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
        Text(
            text = recommended.task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        ) {
            AssistantBadge(
                text = stringResource(recommended.task.category.labelRes()),
                tint = SecondaryCyan,
            )
            AssistantBadge(
                text = stringResource(recommended.task.priority.assistantLabelRes()),
                tint = if (recommended.task.priority == TaskPriority.ALTA) ErrorColor else PrimaryFixedDim,
                iconRes = R.drawable.ic_flag,
            )
            AssistantBadge(text = dueText(recommended), tint = OnSurfaceVariant)
        }
    }
}

@Composable
private fun ExplanationBox(recommended: PlannedTask, explanation: ExplanationState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KronoShapes.small)
            .background(SurfaceContainerLow)
            .padding(KronoSpacing.md)
            // Anuncia el texto cuando la IA termina de redactarlo.
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        when (explanation) {
            // Mientras Gemini responde (hasta 20 s) ya se muestra el motivo de plantilla.
            ExplanationState.Loading -> {
                Text(
                    text = templateExplanation(recommended.factors),
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurface,
                )
                LoadingExplanation()
            }
            is ExplanationState.Generated -> ExplanationText(
                text = explanation.text,
                caption = stringResource(R.string.assistant_explanation_ai),
            )
            ExplanationState.Fallback -> ExplanationText(
                text = templateExplanation(recommended.factors),
                caption = stringResource(R.string.assistant_explanation_template),
            )
        }
    }
}

@Composable
private fun LoadingExplanation() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(KronoSizes.iconSmall),
            color = SecondaryCyan,
            strokeWidth = KronoSizes.progressStroke,
        )
        Text(
            text = stringResource(R.string.assistant_explanation_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun ExplanationText(text: String, caption: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = OnSurface)
    Text(text = caption, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
}

@Composable
private fun TimeBlockBox(recommended: PlannedTask) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(KronoShapes.small)
            .background(SurfaceContainerLow)
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        IconLine(iconRes = R.drawable.ic_schedule, text = estimateText(recommended), color = OnSurface)
        IconLine(iconRes = R.drawable.ic_trending_up, text = timeRangeText(recommended), color = SecondaryCyan)
    }
}

@Composable
private fun IconLine(@DrawableRes iconRes: Int, text: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(KronoSizes.iconSmall),
        )
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun OpenTaskButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
        shape = KronoShapes.small,
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryMagenta, contentColor = OnSurface),
    ) {
        Text(
            text = stringResource(R.string.assistant_open_task),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

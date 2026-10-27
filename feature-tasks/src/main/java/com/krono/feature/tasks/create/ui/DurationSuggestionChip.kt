package com.krono.feature.tasks.create.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.tasks.R
import kotlin.time.Duration

/**
 * Chip "Sugerencia IA: ~X min · Basado en historial" (F-11 CA-1). Al tocarlo se aplica
 * la duración sugerida al formulario.
 *
 * @param suggestion duración sugerida por el historial de la categoría.
 * @param onApply aplica la sugerencia.
 */
@Composable
fun DurationSuggestionChip(
    suggestion: Duration,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.duration_suggestion, suggestion.inWholeMinutes)
    val actionDescription = stringResource(R.string.cd_apply_duration_suggestion, label)

    Row(
        modifier = modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(KronoShapes.small)
            .border(KronoGlass.borderWidth, PrimaryFixedDim, KronoShapes.small)
            .clickable(role = Role.Button, onClick = onApply)
            .semantics { contentDescription = actionDescription }
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
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
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryFixedDim,
        )
    }
}

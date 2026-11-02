package com.krono.feature.tasks.focusmode.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.focusmode.FocusModeUiState

/**
 * Tarjeta con la tarea del enfoque (F-09 CA-1): duración del bloque, nombre, paso actual
 * ("Paso 3 de 4") y progreso de las subtareas. Todo se apila en columna para no perder texto con
 * la fuente grande; sin subtareas no se muestra ningún paso.
 */
@Composable
internal fun FocusTaskCard(state: FocusModeUiState, modifier: Modifier = Modifier) {
    val task = state.task ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        // Sin enfoque en curso (por ejemplo, ya finalizado) no hay bloque que mostrar.
        if (state.phase != null) {
            Text(
                text = stringResource(R.string.focus_mode_block_minutes, state.blockDuration.inWholeMinutes),
                style = MaterialTheme.typography.labelLarge,
                color = SecondaryCyan,
            )
        }
        Text(
            text = task.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
        StepProgress(state)
    }
}

@Composable
private fun StepProgress(state: FocusModeUiState) {
    if (state.totalSteps == 0) return
    val stepText = state.currentStepTitle?.let { title ->
        stringResource(R.string.focus_mode_step, state.currentStep ?: 0, state.totalSteps, title)
    } ?: stringResource(R.string.focus_mode_steps_done, state.totalSteps)
    Text(
        text = stepText,
        style = MaterialTheme.typography.bodyMedium,
        color = OnSurfaceVariant,
    )
    LinearProgressIndicator(
        progress = { state.completedSteps.toFloat() / state.totalSteps },
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape),
        color = PrimaryMagenta,
        trackColor = SurfaceContainerHigh,
    )
}

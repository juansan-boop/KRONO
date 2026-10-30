package com.krono.feature.tasks.focusmode.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.domain.timer.FocusDefaults
import com.krono.core.domain.timer.FocusPhase
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.feature.tasks.R
import com.krono.feature.tasks.timer.ui.MainButton

/**
 * Controles del modo enfoque (F-09 CA-2): el botón principal de pausar o reanudar y, debajo, "+5m
 * Descanso" y "Finalizar". Se apilan en vertical, cada uno al ancho completo, para que con la
 * fuente al 200 % ninguna etiqueta se parta ni se pierda.
 *
 * El botón principal se oculta cuando el bloque llegó a cero: en ese momento solo se puede elegir
 * entre descansar y finalizar en el aviso.
 *
 * @param enabled `false` mientras se procesa una acción, para evitar dobles toques.
 */
@Composable
internal fun FocusModeControls(
    phase: FocusPhase,
    enabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExtraBreak: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
    ) {
        when (phase) {
            FocusPhase.WORKING ->
                MainButton(R.drawable.ic_pause, R.string.cd_focus_mode_pause, enabled, onPause)
            FocusPhase.PAUSED, FocusPhase.ON_BREAK, FocusPhase.BREAK_OVER ->
                MainButton(R.drawable.ic_play_arrow, R.string.cd_focus_mode_resume, enabled, onResume)
            FocusPhase.BLOCK_COMPLETE -> Unit
        }
        val canTakeBreak = phase == FocusPhase.WORKING || phase == FocusPhase.PAUSED
        SecondaryAction(
            label = stringResource(R.string.focus_mode_extra_break, FocusDefaults.extraBreak.inWholeMinutes.toInt()),
            enabled = enabled && canTakeBreak,
            onClick = onExtraBreak,
        )
        SecondaryAction(
            label = stringResource(R.string.focus_mode_finish),
            enabled = enabled,
            onClick = onFinish,
            iconRes = R.drawable.ic_stop,
        )
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.minTouchTarget),
        shape = CircleShape,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(KronoSpacing.xs))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = OnSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

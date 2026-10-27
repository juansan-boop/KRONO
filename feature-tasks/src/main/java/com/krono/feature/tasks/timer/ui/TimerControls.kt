package com.krono.feature.tasks.timer.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.feature.tasks.R
import com.krono.feature.tasks.timer.TimerStatus

/**
 * Controles del cronómetro (F-08 CA-1): botón principal de iniciar, pausar o reanudar y,
 * con una sesión en curso, el botón de finalizar.
 *
 * @param enabled `false` mientras se procesa una acción, para evitar dobles toques.
 */
@Composable
internal fun TimerControls(
    status: TimerStatus,
    enabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.lg, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            TimerStatus.IDLE -> MainButton(R.drawable.ic_play_arrow, R.string.cd_timer_start, enabled, onStart)
            TimerStatus.RUNNING -> MainButton(R.drawable.ic_pause, R.string.cd_timer_pause, enabled, onPause)
            TimerStatus.PAUSED -> MainButton(R.drawable.ic_play_arrow, R.string.cd_timer_resume, enabled, onResume)
        }
        if (status != TimerStatus.IDLE) {
            OutlinedButton(
                onClick = onFinish,
                enabled = enabled,
                modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
                shape = CircleShape,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stop),
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(KronoSizes.iconSmall),
                )
                Spacer(modifier = Modifier.width(KronoSpacing.xs))
                Text(
                    text = stringResource(R.string.timer_finish),
                    style = MaterialTheme.typography.labelLarge,
                    color = OnSurface,
                )
            }
        }
    }
}

@Composable
private fun MainButton(
    @DrawableRes iconRes: Int,
    @StringRes descriptionRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(descriptionRes)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(KronoSizes.timerMainButton)
            .semantics { contentDescription = description },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryMagenta,
            contentColor = OnSurface,
            disabledContainerColor = SurfaceContainerHigh,
            disabledContentColor = OnSurfaceVariant,
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(KronoSizes.iconLarge),
        )
    }
}

package com.krono.feature.tasks.detail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.tasks.R

/**
 * Botón secundario del detalle de tarea que abre el cronómetro de duración real (F-08).
 */
@Composable
internal fun TimerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.cd_task_detail_timer_button)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight)
            .semantics { contentDescription = description },
        shape = KronoShapes.small,
        border = BorderStroke(KronoGlass.borderWidth, PrimaryFixedDim),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_play_arrow),
            contentDescription = null,
            tint = PrimaryFixedDim,
            modifier = Modifier.size(KronoSizes.iconMedium),
        )
        Spacer(modifier = Modifier.width(KronoSpacing.sm))
        Text(
            text = stringResource(R.string.task_detail_timer_button),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryFixedDim,
        )
    }
}

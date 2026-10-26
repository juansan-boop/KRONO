package com.krono.feature.tasks.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.krono.core.domain.task.TaskCategory
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.Tertiary
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Reparto del tiempo planificado entre Estudio, Trabajo y Personal. Reemplaza la
 * "Telemetría Cognitiva" del diseño con un dato que la app sí mide (D-5).
 *
 * @param balance porcentaje por categoría; suma 100.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BalanceCard(balance: Map<TaskCategory, Int>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface()
            .padding(KronoSpacing.md),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.assistant_balance_title),
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
        )
        BalanceBar(balance)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(KronoSpacing.md)) {
            TaskCategory.entries.forEach { category ->
                BalanceLegend(category, balance[category] ?: 0)
            }
        }
        Text(
            text = stringResource(R.string.assistant_balance_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
    }
}

@Composable
private fun BalanceBar(balance: Map<TaskCategory, Int>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(KronoSpacing.sm)
            .clip(CircleShape)
            .background(SurfaceContainerHigh),
    ) {
        // Row.weight exige valores positivos: las categorías en 0 % no ocupan espacio.
        TaskCategory.entries
            .filter { (balance[it] ?: 0) > 0 }
            .forEach { category ->
                Box(
                    modifier = Modifier
                        .weight((balance[category] ?: 0).toFloat())
                        .fillMaxHeight()
                        .background(category.balanceColor()),
                )
            }
    }
}

@Composable
private fun BalanceLegend(category: TaskCategory, percent: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(KronoSpacing.sm)
                .clip(CircleShape)
                .background(category.balanceColor()),
        )
        Text(
            text = stringResource(R.string.assistant_balance_item, stringResource(category.labelRes()), percent),
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
        )
    }
}

private fun TaskCategory.balanceColor(): Color = when (this) {
    TaskCategory.ESTUDIO -> SecondaryCyan
    TaskCategory.TRABAJO -> PrimaryFixedDim
    TaskCategory.PERSONAL -> Tertiary
}

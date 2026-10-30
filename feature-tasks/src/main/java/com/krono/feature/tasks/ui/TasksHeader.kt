package com.krono.feature.tasks.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.krono.core.ui.brand.KronoSymbol
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.feature.tasks.R

/**
 * Cabecera superior de la pantalla de tareas con identidad visual de KRONO.
 *
 * @param onOpenAssistant abre el asistente de prioridades (F-12); si es `null` no se muestra
 * el botón.
 */
@Composable
fun TasksHeader(
    modifier: Modifier = Modifier,
    onOpenAssistant: (() -> Unit)? = null,
) {
    val title: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KronoSymbol(height = KronoSizes.brandSymbol, contentDescription = null)
            Spacer(modifier = Modifier.width(KronoSpacing.md))
            Column {
                Text(
                    text = stringResource(R.string.tasks_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                )
                Text(
                    text = stringResource(R.string.tasks_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            }
        }
    }
    if (onOpenAssistant == null) {
        Box(modifier = modifier.fillMaxWidth().padding(vertical = KronoSpacing.sm)) { title() }
        return
    }
    // Con fuente grande el botón pasa debajo del título en vez de comprimirlo palabra por palabra.
    TrailingRowOrStack(
        modifier = modifier.fillMaxWidth().padding(vertical = KronoSpacing.sm),
        spacing = KronoSpacing.md,
    ) {
        title()
        AssistantButton(onClick = onOpenAssistant)
    }
}

@Composable
private fun AssistantButton(onClick: () -> Unit) {
    val description = stringResource(R.string.cd_open_assistant)
    Row(
        modifier = Modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .clip(KronoShapes.small)
            .border(KronoGlass.borderWidth, PrimaryFixedDim, KronoShapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = KronoSpacing.sm),
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
            text = stringResource(R.string.action_open_assistant),
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryFixedDim,
        )
    }
}

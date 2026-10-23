package com.krono.feature.tasks.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Estado vacío cuando no hay tareas disponibles o no hay coincidencias con el filtro (F-03 CA-4).
 * Presenta una ilustración en Lumina Glass y un botón de acción principal para añadir una tarea.
 */
@Composable
fun TasksEmptyState(
    onAddTask: () -> Unit,
    modifier: Modifier = Modifier,
    isFiltered: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Insignia luminosa con el ícono de checklist
        Box(
            modifier = Modifier
                .size(KronoSizes.brandLogo)
                .glassSurface(cornerRadius = KronoSizes.brandLogo / 2)
                .border(
                    width = KronoGlass.borderWidth,
                    color = PrimaryMagenta.copy(alpha = BADGE_BORDER_ALPHA),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(KronoIcons.Checklist),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconLarge),
            )
        }

        Spacer(modifier = Modifier.height(KronoSpacing.lg))

        // Título del estado vacío
        val title = if (isFiltered) {
            stringResource(R.string.empty_filter_title)
        } else {
            stringResource(R.string.empty_tasks_title)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(KronoSpacing.xs))

        // Subtítulo explicativo
        val subtitle = if (isFiltered) {
            stringResource(R.string.empty_filter_subtitle)
        } else {
            stringResource(R.string.empty_tasks_subtitle)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(KronoSpacing.xl))

        // Botón de acción: Añadir Nueva Tarea (CA-4)
        Button(
            onClick = onAddTask,
            modifier = Modifier
                .fillMaxWidth(BUTTON_MAX_WIDTH_FRACTION)
                .heightIn(min = KronoSizes.controlHeight),
            shape = RoundedCornerShape(KronoSpacing.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryMagenta,
                contentColor = OnSurface,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(KronoSizes.iconMedium),
                tint = OnSurface,
            )
            Spacer(modifier = Modifier.width(KronoSpacing.sm))
            Text(
                text = stringResource(R.string.action_add_task),
                style = MaterialTheme.typography.titleMedium,
                color = OnSurface,
            )
        }
    }
}

private const val BADGE_BORDER_ALPHA = 0.35f
private const val BUTTON_MAX_WIDTH_FRACTION = 0.85f

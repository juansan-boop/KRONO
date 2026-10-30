package com.krono.feature.tasks.focus.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.ErrorContainer
import com.krono.core.ui.theme.JetBrainsMono
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.tasks.R

/**
 * Tarjeta de una plantilla de enfoque (F-10 CA-2, CA-3): nombre, insignia "ACTIVO", minutos de
 * foco y descanso, porcentaje del ciclo y acciones de activar, editar y eliminar. La activa lleva
 * borde y franja magenta; el resto ofrece "Activar".
 *
 * @param template plantilla a mostrar.
 * @param canDelete `false` cuando es la única plantilla y no se puede eliminar.
 */
@Composable
fun FocusTemplateCard(
    template: FocusTemplate,
    canDelete: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (template.isActive) PrimaryMagenta.copy(alpha = ACTIVE_BORDER_ALPHA) else KronoGlass.borderColor
    Row(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(borderColor = borderColor)
            .then(if (template.isActive) Modifier.drawBehind { drawActiveStripe() } else Modifier),
    ) {
        if (template.isActive) Spacer(Modifier.width(KronoSpacing.xs))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(KronoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm)) {
                TemplateSummary(template, Modifier.weight(1f))
                TemplateActions(template, canDelete, onEdit, onDelete)
            }
            FocusRatioBar(template.focusPercent, template.breakPercent)
            if (!template.isActive) ActivateButton(template.name, onActivate)
        }
    }
}

/**
 * Dibuja la franja magenta del borde izquierdo de la tarjeta activa. Se pinta por detrás en lugar
 * de medir la fila con `IntrinsicSize.Min`: el `FlowRow` de las etiquetas no tiene altura
 * intrínseca fiable y, con la fuente al 200 %, la tarjeta quedaba cortada.
 */
private fun DrawScope.drawActiveStripe() {
    drawRect(color = PrimaryMagenta, size = Size(KronoSpacing.xs.toPx(), size.height))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateSummary(template: FocusTemplate, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            if (template.isActive) ActiveBadge()
        }
        Text(
            text = minutesLine(template),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = JetBrainsMono,
            color = OnSurfaceVariant,
        )
    }
}

/** "45m foco · 10m descanso" con la parte de foco resaltada. */
@Composable
private fun minutesLine(template: FocusTemplate) = buildAnnotatedString {
    withStyle(SpanStyle(color = OnSurface, fontWeight = FontWeight.Medium)) {
        append(stringResource(R.string.focus_minutes_focus, template.focusMinutes))
    }
    append(MINUTES_SEPARATOR)
    append(stringResource(R.string.focus_minutes_break, template.breakMinutes))
}

@Composable
private fun ActiveBadge() {
    Row(
        modifier = Modifier
            .clip(KronoShapes.extraSmall)
            .background(SecondaryCyan.copy(alpha = BADGE_BG_ALPHA))
            .border(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = BADGE_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Box(Modifier.size(KronoSpacing.xs + KronoSpacing.xs / 2).clip(CircleShape).background(SecondaryCyan))
        Text(
            text = stringResource(R.string.focus_active_badge),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            color = SecondaryCyan,
        )
    }
}

@Composable
private fun TemplateActions(
    template: FocusTemplate,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val editLabel = stringResource(R.string.focus_cd_edit, template.name)
    val deleteLabel = stringResource(R.string.focus_cd_delete, template.name)
    Row(horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs)) {
        IconButton(
            onClick = onEdit,
            modifier = Modifier
                .size(KronoSizes.minTouchTarget)
                .background(OnSurface.copy(alpha = EDIT_BG_ALPHA), KronoShapes.small)
                .border(KronoGlass.borderWidth, OutlineVariant, KronoShapes.small),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = editLabel,
                tint = OnSurface,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(KronoSizes.minTouchTarget)
                    .background(ErrorContainer.copy(alpha = DELETE_BG_ALPHA), KronoShapes.small)
                    .border(KronoGlass.borderWidth, ErrorColor.copy(alpha = DELETE_BORDER_ALPHA), KronoShapes.small),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = deleteLabel,
                    tint = ErrorColor,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
            }
        }
    }
}

@Composable
private fun ActivateButton(templateName: String, onActivate: () -> Unit) {
    val label = stringResource(R.string.focus_cd_activate, templateName)
    OutlinedButton(
        onClick = onActivate,
        modifier = Modifier
            .heightIn(min = KronoSizes.minTouchTarget)
            .semantics { contentDescription = label },
        shape = KronoShapes.small,
        border = BorderStroke(KronoGlass.borderWidth, SecondaryCyan.copy(alpha = BADGE_BORDER_ALPHA)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryCyan),
    ) {
        Text(text = stringResource(R.string.focus_activate), style = MaterialTheme.typography.labelLarge)
    }
}

private const val MINUTES_SEPARATOR = " · "
private const val ACTIVE_BORDER_ALPHA = 0.6f
private const val BADGE_BG_ALPHA = 0.15f
private const val BADGE_BORDER_ALPHA = 0.35f
private const val EDIT_BG_ALPHA = 0.05f
private const val DELETE_BG_ALPHA = 0.25f
private const val DELETE_BORDER_ALPHA = 0.4f

package com.krono.feature.tasks.assistant.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoShapes
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing

/** Opacidad del fondo de las etiquetas, para que el color se lea como tinte sobre el vidrio. */
private const val BADGE_BACKGROUND_ALPHA = 0.15f

/** Opacidad del borde de las etiquetas. */
private const val BADGE_BORDER_ALPHA = 0.4f

/**
 * Etiqueta compacta del asistente (categoría, prioridad, confianza o duración).
 *
 * @param tint color del texto, del ícono y del tinte de fondo; siempre un token de `core-ui`.
 * @param iconRes ícono opcional a la izquierda del texto.
 */
@Composable
internal fun AssistantBadge(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(KronoShapes.extraSmall)
            .background(tint.copy(alpha = BADGE_BACKGROUND_ALPHA))
            .border(KronoGlass.borderWidth, tint.copy(alpha = BADGE_BORDER_ALPHA), KronoShapes.extraSmall)
            .padding(horizontal = KronoSpacing.sm, vertical = KronoSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconRes?.let {
            Icon(
                painter = painterResource(it),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

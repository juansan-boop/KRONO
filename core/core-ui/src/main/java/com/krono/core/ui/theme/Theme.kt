package com.krono.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/** Esquema de color oscuro del Lumina Glass System, construido solo con los tokens de `Color.kt`. */
private val KronoDarkColorScheme = darkColorScheme(
    primary = PrimaryMagenta,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryFixedDim,
    secondary = SecondaryCyan,
    secondaryContainer = SecondaryFixedDim,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    tertiaryContainer = TertiaryContainer,
    background = DeepMidnight,
    onBackground = OnSurface,
    surface = DeepMidnight,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant,
    error = ErrorColor,
    errorContainer = ErrorContainer,
    onError = OnError,
)

/**
 * Aplica el tema Lumina Glass System a [content].
 *
 * KRONO no tiene tema claro: la preferencia del sistema (`isSystemInDarkTheme()`) se ignora
 * intencionalmente y siempre se aplica el esquema oscuro y la paleta fija, sin color dinámico.
 */
@Composable
fun KronoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KronoDarkColorScheme,
        typography = KronoTypography,
        shapes = KronoShapes,
        content = content,
    )
}

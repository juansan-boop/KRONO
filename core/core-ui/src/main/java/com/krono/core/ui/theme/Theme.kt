package com.krono.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * KRONO no tiene tema claro — isSystemInDarkTheme() se ignora intencionalmente,
 * siempre se aplica el darkColorScheme del Lumina Glass System.
 */
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
 * Aplica el tema Lumina Glass System (siempre oscuro) a [content].
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

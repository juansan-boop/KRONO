package com.krono.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Hanken Grotesk para texto general de UI, JetBrains Mono para datos/números
 * (duraciones, cronómetro, horas) — la mezcla es intencional: monoespaciada
 * evita que los dígitos "salten" mientras corre el cronómetro.
 *
 * TODO: agregar las fuentes reales a res/font/ (Hanken Grotesk + JetBrains Mono,
 * descargadas de Google Fonts) y reemplazar FontFamily.Default por los
 * FontFamily generados con Font(R.font....).
 */
/** Familia para texto general de UI (Hanken Grotesk). */
val HankenGrotesk = FontFamily.Default // placeholder hasta agregar el recurso real
/** Familia para datos y números (JetBrains Mono). */
val JetBrainsMono = FontFamily.Monospace // placeholder hasta agregar el recurso real

/** Escala tipográfica de KRONO; los estilos no definidos usan los valores por defecto de Material. */
val KronoTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    // Texto de botones y acciones destacadas.
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    // Etiquetas de campos y enlaces secundarios.
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
    ),
    // Ayudas y mensajes de validación bajo los campos.
    bodySmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)

package com.krono.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.krono.core.ui.R

/**
 * Tamaños de texto de la escala tipográfica de KRONO.
 *
 * Centralizan los valores en `sp` para que ninguna pantalla ni estilo los escriba
 * como literales sueltos.
 */
object KronoTextSize {
    /** Titulares principales de pantalla. */
    val Headline: TextUnit = 28.sp

    /** Títulos de sección. */
    val Title: TextUnit = 22.sp

    /** Texto base de lectura y de botones o acciones destacadas. */
    val Large: TextUnit = 16.sp

    /** Texto secundario y datos numéricos. */
    val Medium: TextUnit = 14.sp

    /** Etiquetas de campos, ayudas y mensajes de validación. */
    val Small: TextUnit = 12.sp
}

/**
 * Hanken Grotesk para texto general de UI. Se declaran solo los pesos que usa
 * [KronoTypography] (fuentes OFL en `res/font/`, licencia en `core-ui/licenses/`).
 */
val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

/**
 * JetBrains Mono para datos y números (duraciones, cronómetro, horas): al ser
 * monoespaciada evita que los dígitos "salten" mientras corre el cronómetro.
 * Solo se declara el peso que usa [KronoTypography].
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Escala tipográfica de KRONO; los estilos no definidos usan los valores por defecto de Material. */
val KronoTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = KronoTextSize.Headline,
    ),
    titleLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = KronoTextSize.Title,
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = KronoTextSize.Large,
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = KronoTextSize.Medium,
    ),
    labelLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = KronoTextSize.Medium,
    ),
    // Texto de botones y acciones destacadas.
    titleMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = KronoTextSize.Large,
    ),
    // Etiquetas de campos y enlaces secundarios.
    labelMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = KronoTextSize.Small,
    ),
    // Ayudas y mensajes de validación bajo los campos.
    bodySmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = KronoTextSize.Small,
    ),
)

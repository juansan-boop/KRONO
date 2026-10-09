package com.krono.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Lumina Glass System — paleta base (locked in).
 * KRONO es siempre dark, no existe variante light.
 * Tomado directamente de la definición de diseño del proyecto — no editar
 * valores individuales sin actualizar también el export de Stitch/Figma.
 */

// Superficie / fondo
/** Fondo base de la app (Deep Midnight). */
val DeepMidnight = Color(0xFF121221)
/** Superficie de contenedor, nivel bajo. */
val SurfaceContainerLow = Color(0xFF1A1A2A)
/** Superficie de contenedor, nivel base. */
val SurfaceContainer = Color(0xFF1E1E2E)
/** Superficie de contenedor, nivel alto (base del efecto glass). */
val SurfaceContainerHigh = Color(0xFF292839)
/** Superficie de contenedor, nivel más alto. */
val SurfaceContainerHighest = Color(0xFF343344)
/** Variante de superficie para campos y fondos de apoyo. */
val SurfaceVariant = Color(0xFF343344)
/** Superficie brillante para elementos resaltados. */
val SurfaceBright = Color(0xFF383848)
/** Texto e íconos principales sobre superficies. */
val OnSurface = Color(0xFFE3E0F6)
/** Texto e íconos secundarios sobre superficies. */
val OnSurfaceVariant = Color(0xFFD4C0D7)
/** Contorno de énfasis medio. */
val Outline = Color(0xFF9D8BA0)
/** Contorno sutil y divisores. */
val OutlineVariant = Color(0xFF514255)

// Primario — magenta
/** Color primario (magenta). */
val PrimaryMagenta = Color(0xFFBD00FF)
/** Primario atenuado, para acentos y contenedores primarios. */
val PrimaryFixedDim = Color(0xFFECB2FF)
/** Contenido sobre el color primario. */
val OnPrimary = Color(0xFF520071)

// Secundario — cyan
/** Color secundario (cyan). */
val SecondaryCyan = Color(0xFF49D9E5)
/** Secundario atenuado, para contenedores secundarios. */
val SecondaryFixedDim = Color(0xFF00DBE9)
/** Contenido sobre contenedores secundarios. */
val OnSecondaryContainer = Color(0xFF00686F)

// Terciario / acentos
/** Acento terciario. */
val Tertiary = Color(0xFFFFB1C3)
/** Contenedor del acento terciario. */
val TertiaryContainer = Color(0xFFE7006E)

// Error
/** Color de texto e íconos de error. */
val ErrorColor = Color(0xFFFFB4AB)
/** Contenedor de error. */
val ErrorContainer = Color(0xFF93000A)
/** Contenido sobre el color de error. */
val OnError = Color(0xFF690005)

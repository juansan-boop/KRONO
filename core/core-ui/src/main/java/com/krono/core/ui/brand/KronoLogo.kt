package com.krono.core.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.krono.core.ui.R
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SurfaceContainerHighest
import com.krono.core.ui.theme.glassSurface

/** Proporción ancho/alto del símbolo de KRONO (lienzo de 206 x 220 de sus vectores). */
const val KronoSymbolAspectRatio: Float = 206f / 220f

/** Etiqueta para ubicar el símbolo en pruebas de UI. */
const val KronoSymbolTestTag: String = "krono_symbol"

/**
 * Símbolo de KRONO: "K", reloj de arena y "D" con punto. Son dos vectores superpuestos
 * (`krono_symbol_lilac` y `krono_symbol_navy`, compartidos con el ícono del launcher)
 * teñidos con los tokens de Color.kt; los huecos entre piezas dejan ver el fondo.
 *
 * @param height alto del símbolo; el ancho sale de [KronoSymbolAspectRatio].
 * @param contentDescription `null` si es decorativo (por ejemplo, junto a la palabra KRONO).
 */
@Composable
fun KronoSymbol(
    height: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(height)
            .aspectRatio(KronoSymbolAspectRatio)
            .testTag(KronoSymbolTestTag)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Image
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.krono_symbol_lilac),
            contentDescription = null,
            colorFilter = ColorFilter.tint(PrimaryFixedDim),
            modifier = Modifier.matchParentSize(),
        )
        Image(
            painter = painterResource(R.drawable.krono_symbol_navy),
            contentDescription = null,
            colorFilter = ColorFilter.tint(SurfaceContainerHighest),
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * Logo de KRONO: el símbolo con la palabra debajo. La palabra es texto (no un trazo),
 * así que el símbolo queda decorativo y el lector de pantalla lee [wordmark].
 *
 * @param wordmark la palabra de la marca; por defecto, `krono_wordmark` de core-ui.
 */
@Composable
fun KronoLogo(
    modifier: Modifier = Modifier,
    wordmark: String = stringResource(R.string.krono_wordmark),
    symbolHeight: Dp = KronoSizes.brandSymbol,
    wordmarkStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        KronoSymbol(height = symbolHeight, contentDescription = null)
        Text(text = wordmark, style = wordmarkStyle, color = PrimaryFixedDim)
    }
}

@Preview(name = "Logo de KRONO")
@Composable
private fun KronoLogoPreview() {
    KronoTheme {
        Box(
            modifier = Modifier
                .background(DeepMidnight)
                .padding(KronoSpacing.lg)
                .glassSurface()
                .padding(KronoSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            KronoLogo(symbolHeight = KronoSizes.brandSymbolLarge, wordmarkStyle = MaterialTheme.typography.headlineLarge)
        }
    }
}

/** Aproximación del ícono del launcher: lienzo de 108dp y zona segura de 66dp. */
@Preview(name = "Ícono del launcher", widthDp = 108, heightDp = 108)
@Composable
private fun KronoLauncherIconPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentAlignment = Alignment.Center,
    ) {
        KronoSymbol(height = KronoSizes.brandSymbolLarge, contentDescription = null)
    }
}

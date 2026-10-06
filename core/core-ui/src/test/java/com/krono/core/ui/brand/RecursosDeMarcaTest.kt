package com.krono.core.ui.brand

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.SurfaceContainerHighest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Los recursos XML del símbolo (que también usa el ícono del launcher) no pueden leer
 * los tokens de Color.kt; estas pruebas evitan que los dos lados se desincronicen.
 */
class RecursosDeMarcaTest {

    private val res = File("src/main/res")

    private fun xml(path: String): Element {
        val file = File(res, path)
        assertTrue("Falta el recurso ${file.path}", file.exists())
        return DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file).documentElement
    }

    private fun Element.android(name: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", name)

    private fun brandColors(): Map<String, Int> {
        val nodes = xml("values/colors.xml").getElementsByTagName("color")
        return (0 until nodes.length).map { nodes.item(it) as Element }.associate { color ->
            // Colores opacos #RRGGBB, como los tokens de Color.kt.
            color.getAttribute("name") to (0xFF000000 or color.textContent.trim().removePrefix("#").toLong(16)).toInt()
        }
    }

    private fun assertSameColor(token: Color, resource: Int?) =
        assertEquals(Integer.toHexString(token.toArgb()), resource?.let(Integer::toHexString))

    @Test
    fun losColoresDeMarcaCoincidenConLosTokensDeColorKt() {
        val colors = brandColors()

        assertSameColor(PrimaryFixedDim, colors["krono_brand_lilac"])
        assertSameColor(SurfaceContainerHighest, colors["krono_brand_navy"])
        assertSameColor(DeepMidnight, colors["krono_brand_midnight"])
    }

    @Test
    fun cadaCapaDelSimboloUsaSuColorDeMarca() {
        val lilac = xml("drawable/krono_symbol_lilac.xml").getElementsByTagName("path")
        val navy = xml("drawable/krono_symbol_navy.xml").getElementsByTagName("path")

        assertTrue(lilac.length > 0 && navy.length > 0)
        (0 until lilac.length).forEach {
            assertEquals("@color/krono_brand_lilac", (lilac.item(it) as Element).android("fillColor"))
        }
        (0 until navy.length).forEach {
            assertEquals("@color/krono_brand_navy", (navy.item(it) as Element).android("fillColor"))
        }
    }

    @Test
    fun lasDosCapasDelSimboloCompartenLienzoParaSuperponerse() {
        val lilac = xml("drawable/krono_symbol_lilac.xml")
        val navy = xml("drawable/krono_symbol_navy.xml")

        assertEquals(lilac.android("viewportWidth"), navy.android("viewportWidth"))
        assertEquals(lilac.android("viewportHeight"), navy.android("viewportHeight"))
        assertEquals(
            KronoSymbolAspectRatio,
            lilac.android("viewportWidth").toFloat() / lilac.android("viewportHeight").toFloat(),
            0.0001f,
        )
    }
}

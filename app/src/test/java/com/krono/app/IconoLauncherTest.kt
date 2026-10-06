package com.krono.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.hypot

/**
 * Ícono adaptativo de KRONO: capas de fondo, primer plano y monocromática, con el
 * símbolo de core-ui dentro de la zona segura (círculo de 66dp en un lienzo de 108dp).
 */
class IconoLauncherTest {

    private val android = "http://schemas.android.com/apk/res/android"

    private fun xml(file: File): Element {
        assertTrue("Falta ${file.path}", file.exists())
        return DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder().parse(file).documentElement
    }

    private fun res(path: String) = xml(File("src/main/res/$path"))

    private fun Element.attr(name: String): String = getAttributeNS(android, name)

    private fun Element.child(tag: String): Element =
        getElementsByTagName(tag).item(0) as? Element ?: throw AssertionError("Falta <$tag>")

    private fun Element.all(tag: String): List<Element> =
        getElementsByTagName(tag).let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    @Test
    fun elManifiestoDeclaraElIconoYSuVersionRedonda() {
        val application = xml(File("src/main/AndroidManifest.xml")).child("application")

        assertEquals("@mipmap/ic_launcher", application.attr("icon"))
        assertEquals("@mipmap/ic_launcher_round", application.attr("roundIcon"))
    }

    @Test
    fun ambosIconosSonAdaptativosConFondoPrimerPlanoYMonocromatico() {
        listOf("ic_launcher", "ic_launcher_round").forEach { name ->
            val icon = res("mipmap-anydpi/$name.xml")

            assertEquals("adaptive-icon", icon.tagName)
            assertEquals("@color/krono_brand_midnight", icon.child("background").attr("drawable"))
            assertEquals("@drawable/ic_launcher_foreground", icon.child("foreground").attr("drawable"))
            assertEquals("@drawable/ic_launcher_monochrome", icon.child("monochrome").attr("drawable"))
        }
    }

    @Test
    fun elPrimerPlanoSuperponeLasCapasLilaYAzulDelSimbolo() {
        val layers = res("drawable/ic_launcher_foreground.xml").all("inset").map { it.attr("drawable") }

        assertEquals(listOf("@drawable/krono_symbol_lilac", "@drawable/krono_symbol_navy"), layers)
    }

    @Test
    fun laCapaMonocromaticaUsaSoloLaSiluetaLila() {
        val layers = res("drawable/ic_launcher_monochrome.xml").all("inset").map { it.attr("drawable") }

        assertEquals(listOf("@drawable/krono_symbol_lilac"), layers)
    }

    @Test
    fun elSimboloCabeEnLaZonaSeguraSinDeformarse() {
        val canvas = 108.0
        val symbol = xml(File("../core/core-ui/src/main/res/drawable/krono_symbol_lilac.xml"))
        val aspect = symbol.attr("viewportWidth").toDouble() / symbol.attr("viewportHeight").toDouble()

        (res("drawable/ic_launcher_foreground.xml").all("inset") + res("drawable/ic_launcher_monochrome.xml").all("inset"))
            .forEach { inset ->
                fun fraction(name: String) = inset.attr(name).removeSuffix("%").toDouble() / 100
                val width = canvas * (1 - fraction("insetLeft") - fraction("insetRight"))
                val height = canvas * (1 - fraction("insetTop") - fraction("insetBottom"))

                assertEquals("Centrado horizontal", fraction("insetLeft"), fraction("insetRight"), 1e-9)
                assertEquals("Centrado vertical", fraction("insetTop"), fraction("insetBottom"), 1e-9)
                assertTrue("Las esquinas salen de la zona segura", hypot(width / 2, height / 2) <= 33.0)
                assertEquals("Proporción del símbolo", aspect, width / height, aspect * 0.01)
            }
    }
}

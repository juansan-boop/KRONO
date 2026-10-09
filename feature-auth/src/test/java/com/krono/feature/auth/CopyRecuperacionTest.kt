package com.krono.feature.auth

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Vigila el copy de recuperar contraseña (F-24 CA-3, D-10): el envío es real y
 * ningún texto revela si un correo tiene cuenta en KRONO.
 */
class CopyRecuperacionTest {

    private val xml = File("src/main/res/values/strings.xml").readText()

    private fun cadena(nombre: String): String {
        val regex = Regex("""<string name="$nombre">(.*?)</string>""")
        return requireNotNull(regex.find(xml)) { "Falta $nombre" }.groupValues[1]
    }

    @Test
    fun elTituloDeExitoConservaElDelDiseno() {
        assertEquals("¡Enlace Enviado!", cadena("auth_forgot_success_title"))
    }

    @Test
    fun elCuerpoDeExitoEsNeutroSobreSiElCorreoTieneCuenta() {
        assertEquals("Si el correo está registrado, te enviamos un enlace", cadena("auth_forgot_success_body"))
    }

    @Test
    fun ningunTextoDiceQueElCorreoNoEstaRegistrado() {
        assertFalse(xml.lowercase().contains("no está registrado"))
    }

    @Test
    fun ningunTextoDiceQueLaRecuperacionEsSimulada() {
        assertFalse(xml.lowercase().contains("simulad"))
    }
}

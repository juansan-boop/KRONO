package com.krono.feature.auth

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Evita que el copy de recuperación prometa un correo real mientras el envío es simulado. */
class CopyRecuperacionTest {

    private fun cadena(nombre: String): String {
        val xml = File("src/main/res/values/strings.xml").readText()
        val regex = Regex("""<string name="$nombre">(.*?)</string>""")
        return requireNotNull(regex.find(xml)) { "Falta $nombre" }.groupValues[1]
    }

    @Test
    fun elCuerpoDeExitoNoPrometeUnEnvioReal() {
        val cuerpo = cadena("auth_forgot_success_body").lowercase()
        assertFalse(cuerpo.contains("enviamos"))
        assertFalse(cuerpo.contains("enlace"))
    }

    @Test
    fun elAvisoIndicaQueElEnvioEsSimulado() {
        assertTrue(cadena("auth_forgot_success_simulated").contains("simulada"))
    }
}

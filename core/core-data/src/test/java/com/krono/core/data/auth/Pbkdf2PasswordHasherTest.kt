package com.krono.core.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Pbkdf2PasswordHasherTest {

    // Pocas iteraciones para que la prueba sea rápida; la app usa DEFAULT_ITERATIONS.
    private val hasher = Pbkdf2PasswordHasher(iterations = 1_000)

    @Test
    fun elHashNoContieneLaContrasenaEnTextoPlano() {
        val resultado = hasher.hash("krono2026!")

        assertFalse(resultado.hash.contains("krono2026!"))
        assertFalse(resultado.salt.contains("krono2026!"))
    }

    @Test
    fun registraLasIteracionesUsadas() {
        assertEquals(1_000, hasher.hash("krono2026!").iterations)
    }

    @Test
    fun laMismaContrasenaProduceSalYHashDistintosEnCadaLlamada() {
        val primero = hasher.hash("krono2026!")
        val segundo = hasher.hash("krono2026!")

        assertNotEquals(primero.salt, segundo.salt)
        assertNotEquals(primero.hash, segundo.hash)
    }

    @Test
    fun verificaLaContrasenaCorrecta() {
        val guardado = hasher.hash("krono2026!")

        assertTrue(hasher.verify("krono2026!", guardado))
    }

    @Test
    fun rechazaUnaContrasenaIncorrecta() {
        val guardado = hasher.hash("krono2026!")

        assertFalse(hasher.verify("krono2026?", guardado))
        assertFalse(hasher.verify("", guardado))
    }

    @Test
    fun verificaConLasIteracionesGuardadasAunqueElHasherUseOtras() {
        val guardado = hasher.hash("krono2026!")

        assertTrue(Pbkdf2PasswordHasher(iterations = 2_000).verify("krono2026!", guardado))
    }

    @Test
    fun laSalTieneDieciseisBytes() {
        val sal = java.util.Base64.getDecoder().decode(hasher.hash("krono2026!").salt)

        assertEquals(16, sal.size)
    }
}

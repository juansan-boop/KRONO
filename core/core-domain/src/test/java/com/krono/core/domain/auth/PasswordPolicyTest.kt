package com.krono.core.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun unaContrasenaVaciaNoCumpleNingunRequisito() {
        val resultado = PasswordPolicy.evaluate("")

        assertEquals(PasswordRequirements(minLength = false, hasDigit = false, hasSpecialChar = false), resultado)
        assertFalse(resultado.isSatisfied)
    }

    @Test
    fun ochoCaracteresCumplenElMinimoDeLongitud() {
        assertTrue(PasswordPolicy.evaluate("abcdefgh").minLength)
        assertFalse(PasswordPolicy.evaluate("abcdefg").minLength)
    }

    @Test
    fun detectaAlMenosUnNumero() {
        assertTrue(PasswordPolicy.evaluate("abc1").hasDigit)
        assertFalse(PasswordPolicy.evaluate("abcd").hasDigit)
    }

    @Test
    fun detectaAlMenosUnCaracterEspecial() {
        assertTrue(PasswordPolicy.evaluate("abc!").hasSpecialChar)
        assertTrue(PasswordPolicy.evaluate("abc_").hasSpecialChar)
        assertFalse(PasswordPolicy.evaluate("abc1").hasSpecialChar)
    }

    @Test
    fun unEspacioNoCuentaComoCaracterEspecial() {
        assertFalse(PasswordPolicy.evaluate("abc def").hasSpecialChar)
    }

    @Test
    fun cumpleLaPoliticaSoloSiCumpleLosTresRequisitos() {
        assertTrue(PasswordPolicy.evaluate("krono2026!").isSatisfied)
        assertFalse(PasswordPolicy.evaluate("krono2026").isSatisfied)
        assertFalse(PasswordPolicy.evaluate("kronokrono!").isSatisfied)
        assertFalse(PasswordPolicy.evaluate("kr0no!").isSatisfied)
    }
}

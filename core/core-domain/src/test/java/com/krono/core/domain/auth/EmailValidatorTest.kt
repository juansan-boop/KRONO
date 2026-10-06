package com.krono.core.domain.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidatorTest {

    @Test
    fun aceptaUnCorreoConUsuarioDominioYExtension() {
        assertTrue(EmailValidator.isValid("ana@correo.com"))
    }

    @Test
    fun aceptaSubdominiosYCaracteresHabitualesDelUsuario() {
        assertTrue(EmailValidator.isValid("ana.perez+u_1@est.unal.edu.co"))
    }

    @Test
    fun ignoraEspaciosAlInicioYAlFinal() {
        assertTrue(EmailValidator.isValid("  ana@correo.com  "))
    }

    @Test
    fun rechazaUnCorreoVacioOEnBlanco() {
        assertFalse(EmailValidator.isValid(""))
        assertFalse(EmailValidator.isValid("   "))
    }

    @Test
    fun rechazaUnCorreoSinArroba() {
        assertFalse(EmailValidator.isValid("anacorreo.com"))
    }

    @Test
    fun rechazaUnCorreoSinDominioOSinExtension() {
        assertFalse(EmailValidator.isValid("ana@"))
        assertFalse(EmailValidator.isValid("ana@correo"))
        assertFalse(EmailValidator.isValid("@correo.com"))
    }

    @Test
    fun rechazaEspaciosInternos() {
        assertFalse(EmailValidator.isValid("ana perez@correo.com"))
    }

    @Test
    fun normalizaQuitandoEspaciosYPasandoAMinusculas() {
        org.junit.Assert.assertEquals("ana@correo.com", EmailValidator.normalize("  Ana@Correo.COM "))
    }
}

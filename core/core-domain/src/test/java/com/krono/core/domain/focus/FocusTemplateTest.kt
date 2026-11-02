package com.krono.core.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del modelo [FocusTemplate], su validación y las plantillas por defecto (F-10 CA-1, CA-3). */
class FocusTemplateTest {

    private fun template(focus: Int, rest: Int) =
        FocusTemplate(id = "t", name = "Plantilla", focusMinutes = focus, breakMinutes = rest, isActive = false)

    @Test
    fun porcentajes_estudioProfundo_son82y18() {
        val estudio = template(focus = 45, rest = 10)

        assertEquals(82, estudio.focusPercent)
        assertEquals(18, estudio.breakPercent)
    }

    @Test
    fun porcentajes_sprintRapido_son83y17() {
        val sprint = template(focus = 25, rest = 5)

        assertEquals(83, sprint.focusPercent)
        assertEquals(17, sprint.breakPercent)
    }

    @Test
    fun porcentajes_siempreSuman100() {
        for (focus in 1..180) for (rest in 1..60) {
            val t = template(focus, rest)
            assertEquals("$focus/$rest", 100, t.focusPercent + t.breakPercent)
        }
    }

    @Test
    fun duracionDelCiclo_sumaFocoYDescanso() {
        assertEquals(55, template(45, 10).cycleMinutes)
    }

    @Test
    fun porDefecto_sonLasTresPlantillasDelSpecConEstudioProfundoActiva() {
        val defaults = DefaultFocusTemplates.all

        assertEquals(
            listOf("Estudio Profundo", "Sprint Rápido", "Lectura Nocturna"),
            defaults.map { it.name },
        )
        assertEquals(listOf(45 to 10, 25 to 5, 60 to 15), defaults.map { it.focusMinutes to it.breakMinutes })
        assertEquals(listOf("Estudio Profundo"), defaults.filter { it.isActive }.map { it.name })
        assertEquals(defaults.size, defaults.map { it.id }.toSet().size)
    }

    @Test
    fun validar_datosCorrectos_noDaErrores() {
        val result = FocusTemplateValidator.validate("Estudio", 45, 10)

        assertTrue(result.isValid)
        assertNull(result.nameError)
    }

    @Test
    fun validar_nombreEnBlancoOMuyLargo_daError() {
        assertEquals(FocusTemplateError.BlankName, FocusTemplateValidator.validate("   ", 45, 10).nameError)
        val largo = "a".repeat(FocusTemplateLimits.MAX_NAME_LENGTH + 1)
        assertEquals(FocusTemplateError.NameTooLong, FocusTemplateValidator.validate(largo, 45, 10).nameError)
        assertTrue(FocusTemplateValidator.validate("a".repeat(FocusTemplateLimits.MAX_NAME_LENGTH), 45, 10).isValid)
    }

    @Test
    fun validar_minutosFueraDeRango_dan_errorPorCampo() {
        assertEquals(FocusTemplateError.InvalidFocusMinutes, FocusTemplateValidator.validate("A", 0, 10).focusError)
        assertEquals(
            FocusTemplateError.InvalidFocusMinutes,
            FocusTemplateValidator.validate("A", FocusTemplateLimits.MAX_FOCUS_MINUTES + 1, 10).focusError,
        )
        assertEquals(FocusTemplateError.InvalidBreakMinutes, FocusTemplateValidator.validate("A", 45, 0).breakError)
        assertEquals(
            FocusTemplateError.InvalidBreakMinutes,
            FocusTemplateValidator.validate("A", 45, FocusTemplateLimits.MAX_BREAK_MINUTES + 1).breakError,
        )
        assertTrue(
            FocusTemplateValidator.validate(
                "A",
                FocusTemplateLimits.MAX_FOCUS_MINUTES,
                FocusTemplateLimits.MAX_BREAK_MINUTES,
            ).isValid,
        )
    }
}

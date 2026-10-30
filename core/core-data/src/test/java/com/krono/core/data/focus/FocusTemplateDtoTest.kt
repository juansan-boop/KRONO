package com.krono.core.data.focus

import com.krono.core.domain.focus.FocusTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas de la conversión entre [FocusTemplate] y el documento de Firestore (F-10). */
class FocusTemplateDtoTest {

    private val template = FocusTemplate("t1", "Estudio Profundo", 45, 10, isActive = true)

    @Test
    fun aMapa_yDeVuelta_conservaTodosLosCampos() {
        val map = FocusTemplateDto.fromDomain(template).toMap()

        assertEquals(template, FocusTemplateDto.fromMap("t1", map).toDomain())
    }

    @Test
    fun aMapa_usaLosNombresDeCampoDelDocumento() {
        val map = FocusTemplateDto.fromDomain(template).toMap()

        assertEquals(
            mapOf(
                "id" to "t1",
                "name" to "Estudio Profundo",
                "focusMinutes" to 45,
                "breakMinutes" to 10,
                "isActive" to true,
            ),
            map,
        )
    }

    @Test
    fun deMapa_losNumerosDeFirestoreLlegaComoLong_seConviertenAEntero() {
        val map = mapOf("name" to "Sprint", "focusMinutes" to 25L, "breakMinutes" to 5L, "isActive" to false)

        assertEquals(FocusTemplate("t2", "Sprint", 25, 5, isActive = false), FocusTemplateDto.fromMap("t2", map).toDomain())
    }

    @Test
    fun deMapa_conCamposFaltantes_usaValoresSeguros() {
        val domain = FocusTemplateDto.fromMap("t3", emptyMap()).toDomain()

        assertEquals("t3", domain.id)
        assertEquals(false, domain.isActive)
    }
}

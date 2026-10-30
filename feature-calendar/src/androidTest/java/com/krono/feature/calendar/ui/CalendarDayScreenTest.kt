package com.krono.feature.calendar.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import com.krono.core.domain.task.Task
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.hours

/**
 * Pruebas de UI de la vista Día (F-16 CA-1 y CA-3) según `calendardayscreen_vista_diaria_krono`:
 * fecha del día, franja de la semana, tareas "En curso" y separador "Ahora · HH:mm".
 */
class CalendarDayScreenTest : CalendarScreenTestBase() {

    @Before
    fun givenDayTasks() {
        givenDesignTasks()
        // Bloque del jueves 22:00 al viernes 01:00: cruza la medianoche.
        repository.givenTasks(
            Task(
                id = "noche",
                title = NIGHT_TITLE,
                dueDate = LocalDateTime(2024, 10, 25, 1, 0),
                estimatedDuration = 3.hours,
            ),
        )
    }

    private fun showDay(fontScale: Float? = null, widthDp: Dp? = null) {
        show(fontScale = fontScale, widthDp = widthDp)
        selectView(R.string.calendar_view_day)
    }

    @Test
    fun alElegirDia_muestraLaFechaLaFranjaYLaAgendaDeHoy() {
        showDay()

        composeRule.onNodeWithText(text(R.string.calendar_view_day)).assertIsSelected()
        composeRule.onNodeWithText("JUEVES · SEMANA 43").assertIsDisplayed()
        composeRule.onNodeWithText("24 de octubre").assertIsDisplayed()
        assertEquals(WEEK_DAYS, allDayCells().fetchSemanticsNodes().size)
        dayCell("jueves, 24 de octubre de 2024").assertIsSelected()
        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(SUPPORT_TITLE).assertExists()
        composeRule.onNodeWithText(REPORT_TITLE).assertDoesNotExist()
    }

    @Test
    fun laTareaCuyoIntervaloIncluyeLaHoraActual_seMarcaEnCursoYLasDemasNo() {
        showDay()

        scrollTo(CLASS_TITLE)
        // Solo el soporte (13:00 a 15:00) incluye las 14:00; la clase (15:00 a 16:30) todavía no.
        assertEquals(
            1,
            composeRule.onAllNodesWithText(text(R.string.calendar_status_in_progress), useUnmergedTree = true)
                .fetchSemanticsNodes().size,
        )
        composeRule.onNodeWithText("13:00 - 15:00", useUnmergedTree = true).assertExists()
    }

    @Test
    fun elSeparadorAhoraMuestraLaHoraYVaAntesDeLaTareaEnCurso() {
        showDay()

        scrollTo(CLASS_TITLE)
        val nowTop = composeRule.onNodeWithText("Ahora · 14:00").getBoundsInRoot().top
        val supportTop = composeRule.onNodeWithText(SUPPORT_TITLE).getBoundsInRoot().top
        val classTop = composeRule.onNodeWithText(CLASS_TITLE).getBoundsInRoot().top
        assertTrue("El separador debe ir antes del soporte en curso", nowTop < supportTop)
        assertTrue("El soporte va antes que la clase", supportTop < classTop)
    }

    @Test
    fun enOtroDia_noHayTareaEnCursoNiSeparadorAhora() {
        showDay()

        clickButton(R.string.calendar_next_day)

        composeRule.onNodeWithText("VIERNES · SEMANA 43").assertIsDisplayed()
        composeRule.onNodeWithText("25 de octubre").assertIsDisplayed()
        dayCell("viernes, 25 de octubre de 2024").assertIsSelected()
        composeRule.onNodeWithText("Ahora", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.calendar_status_in_progress), useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun unaTareaQueCruzaLaMedianoche_apareceEnAmbosDias() {
        showDay()

        scrollTo(NIGHT_TITLE)
        composeRule.onNodeWithText(NIGHT_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("22:00 - 01:00", useUnmergedTree = true).assertExists()

        clickButton(R.string.calendar_next_day)

        scrollTo(NIGHT_TITLE)
        composeRule.onNodeWithText(NIGHT_TITLE).assertIsDisplayed()
    }

    @Test
    fun alCruzarElDomingo_eldiaSiguienteAbreLaSemanaSiguiente() {
        showDay()
        dayCell("domingo, 27 de octubre de 2024").performClick()

        clickButton(R.string.calendar_next_day)

        composeRule.onNodeWithText("LUNES · SEMANA 44").assertIsDisplayed()
        composeRule.onNodeWithText("28 de octubre").assertIsDisplayed()
        dayCell("lunes, 28 de octubre de 2024").assertIsSelected()
    }

    @Test
    fun anteriorYHoy_recorrenLosDiasYRegresan() {
        showDay()

        clickButton(R.string.calendar_previous_day)
        composeRule.onNodeWithText("23 de octubre").assertIsDisplayed()
        composeRule.onNodeWithText("MIÉRCOLES · SEMANA 43").assertIsDisplayed()

        clickButton(R.string.calendar_today_description)
        composeRule.onNodeWithText("24 de octubre").assertIsDisplayed()
        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText("Ahora · 14:00").assertExists()
    }

    @Test
    fun alTocarUnDiaDeLaFranja_seMuestraEseDia() {
        showDay()

        dayCell("martes, 29 de octubre de 2024").assertDoesNotExist()
        dayCell("miércoles, 23 de octubre de 2024").performClick()

        composeRule.onNodeWithText("23 de octubre").assertIsDisplayed()
    }

    @Test
    fun losBotonesDeDiaTienenDescripcionYAreaDe48dp() {
        showDay()

        assertNavigationButtonsHaveMinTouchTarget(R.string.calendar_previous_day, R.string.calendar_next_day)
    }

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_cadaDiaDeLaFranjaMideAlMenos48dp() {
        assertEveryDayCellHasMinTouchTarget(WEEK_DAYS, fontScale = MAX_FONT_SCALE, view = R.string.calendar_view_day)
    }

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_lasEtiquetasDeLaFranjaNoSeTocanYCabenEnLaFranja() =
        assertStripLabelsAreSeparated(MAX_FONT_SCALE, view = R.string.calendar_view_day)

    @Test
    fun conFuenteAl200PorCiento_ningunTextoSeRecortaNiPartePalabras() {
        showDay(fontScale = MAX_FONT_SCALE)

        listOf("JUEVES · SEMANA 43", "24 de octubre").forEach(::assertTextFits)
        scrollTo(CLASS_TITLE)
        listOf("Ahora · 14:00", SUPPORT_TITLE, text(R.string.calendar_status_in_progress)).forEach(::assertTextFits)
    }

    @Test
    fun siNoSePuedenLeerLasTareas_elDiaMuestraErrorYReintentarLasRecupera() {
        repository.observeFailure = IllegalStateException(TECHNICAL_ERROR)
        showDay()

        scrollTo(text(R.string.calendar_retry))
        composeRule.onNodeWithText(text(R.string.calendar_load_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Ahora", substring = true).assertDoesNotExist()

        repository.observeFailure = null
        composeRule.onNodeWithText(text(R.string.calendar_retry)).performClick()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText("Ahora · 14:00").assertExists()
    }

    private companion object {
        const val WEEK_DAYS = 7
        const val NIGHT_TITLE = "Repaso nocturno"
    }
}

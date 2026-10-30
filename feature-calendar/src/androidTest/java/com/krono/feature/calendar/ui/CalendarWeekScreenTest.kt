package com.krono.feature.calendar.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import com.krono.core.domain.task.Task
import com.krono.feature.calendar.R
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Pruebas de UI de la vista Semana (F-16 CA-1) según `calendarweekscreen_vista_semanal_krono`:
 * encabezado "Semana N" con su rango, franja de siete días y agenda del día elegido.
 */
class CalendarWeekScreenTest : CalendarScreenTestBase() {

    @Before
    fun givenWeekTasks() {
        givenDesignTasks()
        repository.givenTasks(Task(id = "entrega", title = DELIVERY_TITLE, dueDate = LocalDateTime(2024, 10, 25, 10, 0)))
    }

    private fun showWeek(fontScale: Float? = null, widthDp: Dp? = null) {
        show(fontScale = fontScale, widthDp = widthDp)
        selectView(R.string.calendar_view_week)
    }

    @Test
    fun alElegirSemana_muestraElNumeroElRangoLaFranjaDeSieteDiasYLaAgendaDeHoy() {
        showWeek()

        composeRule.onNodeWithText("SEMANA 43").assertIsDisplayed()
        composeRule.onNodeWithText("21 - 27 de octubre").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_view_week)).assertIsSelected()
        assertEquals(WEEK_DAYS, allDayCells().fetchSemanticsNodes().size)
        listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM").forEach {
            composeRule.onNodeWithText(it, useUnmergedTree = true).assertExists()
        }
        dayCell("jueves, 24 de octubre de 2024, 2 tareas").assertIsSelected()
        dayCell("viernes, 25 de octubre de 2024, 1 tarea").assertExists()
        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(SUPPORT_TITLE).assertExists()
        composeRule.onNodeWithText("Jueves 24").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_status_in_progress), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("Ahora", substring = true).assertDoesNotExist()
    }

    @Test
    fun alTocarUnDiaDeLaFranja_laAgendaMuestraLasTareasDeEseDia() {
        showWeek()

        dayCell("viernes, 25 de octubre de 2024").performClick()

        dayCell("viernes, 25 de octubre de 2024").assertIsSelected()
        scrollTo(DELIVERY_TITLE)
        composeRule.onNodeWithText(DELIVERY_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(CLASS_TITLE).assertDoesNotExist()
    }

    @Test
    fun lasFlechasCambianDeSemanaYHoyRegresa() {
        showWeek()

        clickButton(R.string.calendar_next_week)
        composeRule.onNodeWithText("SEMANA 44").assertIsDisplayed()
        // La semana del 28 de octubre al 3 de noviembre cruza de mes.
        composeRule.onNodeWithText("28 oct - 3 nov").assertIsDisplayed()
        dayCell("jueves, 31 de octubre de 2024").assertIsSelected()
        dayCell("martes, 29 de octubre de 2024").performClick()
        scrollTo(REPORT_TITLE)
        composeRule.onNodeWithText(REPORT_TITLE).assertIsDisplayed()

        clickButton(R.string.calendar_previous_week)
        clickButton(R.string.calendar_previous_week)
        composeRule.onNodeWithText("SEMANA 42").assertIsDisplayed()
        composeRule.onNodeWithText("14 - 20 de octubre").assertIsDisplayed()

        clickButton(R.string.calendar_today_description)
        composeRule.onNodeWithText("21 - 27 de octubre").assertIsDisplayed()
        dayCell("jueves, 24 de octubre de 2024").assertIsSelected()
    }

    @Test
    fun losBotonesDeSemanaTienenDescripcionYAreaDe48dp() {
        showWeek()

        assertNavigationButtonsHaveMinTouchTarget(R.string.calendar_previous_week, R.string.calendar_next_week)
    }

    @Test
    fun aAnchoDe360dp_cadaDiaDeLaFranjaMideAlMenos48dp() = assertWeekStripTouchTargets(null)

    @Test
    fun aAnchoDe360dpYFuenteAl130PorCiento_cadaDiaDeLaFranjaMideAlMenos48dp() =
        assertWeekStripTouchTargets(LARGE_FONT_SCALE)

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_cadaDiaDeLaFranjaMideAlMenos48dp() =
        assertWeekStripTouchTargets(MAX_FONT_SCALE)

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_lasEtiquetasDeLaFranjaNoSeTocanYCabenEnLaFranja() =
        assertStripLabelsAreSeparated(MAX_FONT_SCALE, view = R.string.calendar_view_week)

    @Test
    fun aAnchoDe360dpYFuenteNormal_lasEtiquetasDeLaFranjaNoSeTocanYCabenEnLaFranja() =
        assertStripLabelsAreSeparated(fontScale = null, view = R.string.calendar_view_week)

    private fun assertWeekStripTouchTargets(fontScale: Float?) =
        assertEveryDayCellHasMinTouchTarget(WEEK_DAYS, fontScale, view = R.string.calendar_view_week)

    @Test
    fun conFuenteAl200PorCiento_ningunTextoDelEncabezadoSeRecortaNiPartePalabras() {
        showWeek(fontScale = MAX_FONT_SCALE)

        clickButton(R.string.calendar_next_week)

        listOf("SEMANA 44", "28 oct - 3 nov", "Jueves 31").forEach(::assertTextFits)
    }

    @Test
    fun siNoSePuedenLeerLasTareas_laSemanaMuestraErrorYReintentarLasRecupera() {
        repository.observeFailure = IllegalStateException(TECHNICAL_ERROR)
        showWeek()

        scrollTo(text(R.string.calendar_retry))
        composeRule.onNodeWithText(text(R.string.calendar_load_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(TECHNICAL_ERROR, substring = true).assertDoesNotExist()
        dayCell("jueves, 24 de octubre de 2024").assertIsSelected()

        repository.observeFailure = null
        composeRule.onNodeWithText(text(R.string.calendar_retry)).performClick()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(CLASS_TITLE).assertIsDisplayed()
    }

    @Test
    fun alTocarUnaTareaDeLaAgenda_seAbreSuDetalle() {
        showWeek()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(CLASS_TITLE).performClick()

        assertEquals(listOf("clase"), openedTasks)
    }

    @Test
    fun laFranjaTieneDescripcionParaLectoresDePantalla() {
        showWeek()

        composeRule.onNodeWithContentDescription(text(R.string.calendar_week_strip)).assertExists()
    }

    @Test
    fun unaSemanaQueCruzaDeMes_tituloAbreviadoEnUnaLineaYSelectorALaAlturaDeMes() {
        show(widthDp = NARROW_SCREEN_WIDTH)
        val monthSelectorTop = selectorTop()

        selectView(R.string.calendar_view_week)
        clickButton(R.string.calendar_next_week)

        composeRule.onNodeWithText("28 oct - 3 nov").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("28 de octubre - 3 de noviembre").assertExists()
        assertTextFits("28 oct - 3 nov")
        assertEquals(monthSelectorTop, selectorTop(), SELECTOR_TOP_TOLERANCE_PX)
    }

    private fun selectorTop(): Float =
        composeRule.onNodeWithContentDescription(text(R.string.calendar_view_selector)).fetchSemanticsNode()
            .boundsInRoot.top

    private companion object {
        const val SELECTOR_TOP_TOLERANCE_PX = 1f
        const val WEEK_DAYS = 7
        const val DELIVERY_TITLE = "Entrega del viernes"
    }
}

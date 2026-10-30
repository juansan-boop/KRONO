package com.krono.feature.calendar.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.krono.feature.calendar.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de UI de la vista mensual (F-16 CA-1 y CA-2) con el ViewModel real sobre un
 * repositorio en memoria y la hora fijada el jueves 24 de octubre de 2024 a las 14:00.
 */
class CalendarMonthScreenTest : CalendarScreenTestBase() {

    @Test
    fun laRejillaMuestraElMesConSusSemanasCompletasYDescribeCadaDia() {
        givenDesignTasks()
        show()

        composeRule.onNodeWithText("Octubre 2024").assertIsDisplayed()
        // Octubre de 2024 ocupa cinco semanas: del lunes 30 de septiembre al domingo 3 de noviembre.
        assertEquals(DAYS_IN_FIVE_WEEKS, allDayCells().fetchSemanticsNodes().size)
        composeRule.onNodeWithContentDescription("jueves, 24 de octubre de 2024, 2 tareas").assertIsSelected()
        composeRule.onNodeWithContentDescription("sábado, 26 de octubre de 2024, sin tareas").assertExists()
        composeRule.onNodeWithContentDescription("lunes, 30 de septiembre de 2024, sin tareas").assertExists()
    }

    @Test
    fun losBotonesDeMesTienenDescripcionAreaDe48dpYCambianElMes() {
        show()

        assertNavigationButtonsHaveMinTouchTarget(R.string.calendar_previous_month, R.string.calendar_next_month)
        clickButton(R.string.calendar_next_month)
        composeRule.onNodeWithText("Noviembre 2024").assertIsDisplayed()

        clickButton(R.string.calendar_previous_month)
        clickButton(R.string.calendar_previous_month)
        composeRule.onNodeWithText("Septiembre 2024").assertIsDisplayed()

        clickButton(R.string.calendar_today_description)
        composeRule.onNodeWithText("Octubre 2024").assertIsDisplayed()
    }

    @Test
    fun aAnchoDe360dp_cadaCeldaDeDiaMideAlMenos48dp() =
        assertEveryDayCellHasMinTouchTarget(DAYS_IN_FIVE_WEEKS, fontScale = null)

    @Test
    fun aAnchoDe360dpYFuenteAl130PorCiento_cadaCeldaDeDiaMideAlMenos48dp() =
        assertEveryDayCellHasMinTouchTarget(DAYS_IN_FIVE_WEEKS, fontScale = LARGE_FONT_SCALE)

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_cadaCeldaDeDiaMideAlMenos48dp() =
        assertEveryDayCellHasMinTouchTarget(DAYS_IN_FIVE_WEEKS, fontScale = MAX_FONT_SCALE)

    @Test
    fun alAbrir_laAgendaMuestraLasTareasDeHoyConSuHorarioYEstado() {
        givenDesignTasks()
        show()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(SUPPORT_TITLE).assertExists()
        composeRule.onNodeWithText("13:00 - 15:00", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText("15:00 - 16:30", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(text(R.string.calendar_status_in_progress), useUnmergedTree = true).assertExists()
        composeRule.onNodeWithText(REPORT_TITLE).assertDoesNotExist()
    }

    @Test
    fun alTocarUnDia_laAgendaListaLasTareasDeEseDia() {
        givenDesignTasks()
        show()

        dayCell("martes, 29 de octubre de 2024").performClick()

        scrollTo(REPORT_TITLE)
        composeRule.onNodeWithText(REPORT_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(CLASS_TITLE).assertDoesNotExist()
        dayCell("martes, 29 de octubre de 2024").assertIsSelected()
    }

    @Test
    fun unDiaSinTareas_muestraElEstadoVacio() {
        givenDesignTasks()
        show()

        dayCell("sábado, 26 de octubre de 2024").performClick()

        scrollTo(text(R.string.calendar_agenda_empty))
        composeRule.onNodeWithText(text(R.string.calendar_agenda_empty)).assertIsDisplayed()
    }

    @Test
    fun alTocarUnaTarea_seAbreSuDetalle() {
        givenDesignTasks()
        show()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(CLASS_TITLE).performClick()

        assertEquals(listOf("clase"), openedTasks)
    }

    @Test
    fun siNoSePuedenLeerLasTareas_muestraErrorConReintentarSinTextoTecnico() {
        repository.observeFailure = IllegalStateException(TECHNICAL_ERROR)
        show()

        scrollTo(text(R.string.calendar_retry))
        composeRule.onNodeWithText(text(R.string.calendar_load_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.calendar_agenda_empty)).assertDoesNotExist()
        composeRule.onNodeWithText(TECHNICAL_ERROR, substring = true).assertDoesNotExist()

        repository.observeFailure = null
        givenDesignTasks()
        composeRule.onNodeWithText(text(R.string.calendar_retry)).performClick()

        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(CLASS_TITLE).assertIsDisplayed()
    }

    @Test
    fun elSelectorOfreceLasTresVistasHabilitadasConMesSeleccionadoYSinProximamente() {
        show()

        composeRule.onNodeWithText(text(R.string.calendar_view_month)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.calendar_view_week)).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.calendar_view_day)).assertIsEnabled()
        composeRule.onNodeWithText("Próximamente", substring = true).assertDoesNotExist()
        composeRule.onNode(hasContentDescription("Próximamente", substring = true)).assertDoesNotExist()
    }

    @Test
    fun enHorizontal_laRejillaYLaAgendaSeAlcanzanConScroll() {
        givenDesignTasks()
        show(widthDp = LANDSCAPE_WIDTH, heightDp = LANDSCAPE_HEIGHT)

        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasContentDescription(LAST_GRID_DAY, substring = true))
        dayCell(LAST_GRID_DAY).assertIsDisplayed()
        scrollTo(CLASS_TITLE)
        composeRule.onNodeWithText(CLASS_TITLE).assertIsDisplayed()
    }

    @Test
    fun conFuenteAl200PorCiento_ningunTextoSeRecortaNiPartePalabras() {
        givenDesignTasks()
        show(fontScale = MAX_FONT_SCALE)
        // Septiembre es el nombre de mes más largo.
        clickButton(R.string.calendar_previous_month)
        clickButton(R.string.calendar_today_description)

        listOf("Octubre 2024", "24", "Jueves 24").forEach(::assertTextFits)
        scrollTo(CLASS_TITLE)
        listOf(CLASS_TITLE, "15:00 - 16:30", text(R.string.calendar_status_in_progress)).forEach(::assertTextFits)
    }

    @Test
    fun conFuenteAl200PorCiento_elTituloDeSeptiembreNoPartePalabras() {
        show(fontScale = MAX_FONT_SCALE)

        clickButton(R.string.calendar_previous_month)

        assertTextFits("Septiembre 2024")
    }

    @Test
    fun alCambiarDeVista_elDiaSeleccionadoSeConservaYAlVolverAMesSigueMarcado() {
        givenDesignTasks()
        show()
        dayCell("martes, 29 de octubre de 2024").performClick()

        selectView(R.string.calendar_view_day)
        composeRule.onNodeWithText("29 de octubre").assertIsDisplayed()
        selectView(R.string.calendar_view_month)

        composeRule.onNodeWithText("Octubre 2024").assertIsDisplayed()
        dayCell("martes, 29 de octubre de 2024").assertIsSelected()
    }

    private companion object {
        const val DAYS_IN_FIVE_WEEKS = 35
        const val LAST_GRID_DAY = "domingo, 3 de noviembre de 2024"
    }
}

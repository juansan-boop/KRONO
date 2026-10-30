package com.krono.feature.calendar.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.calendar.ObserveCalendarUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.calendar.R
import com.krono.feature.calendar.CalendarViewModel
import com.krono.feature.calendar.month.ui.CalendarRoute
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.ceil
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Pruebas de UI de la vista mensual (F-16 CA-1 y CA-2) con el ViewModel real sobre un
 * repositorio en memoria y la hora fijada el jueves 24 de octubre de 2024 a las 14:00.
 */
class CalendarMonthScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeTaskRepository()
    private val openedTasks = mutableListOf<String>()

    private fun text(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun show(fontScale: Float? = null, widthDp: Dp? = null, heightDp: Dp? = null) {
        val viewModel = CalendarViewModel(
            observeCalendar = ObserveCalendarUseCase(repository),
            dateTimeProvider = CurrentDateTimeProvider(FixedClock) { TimeZone.UTC },
        )
        composeRule.setContent {
            val density = LocalDensity.current
            val scaled = fontScale?.let { Density(density.density, it) } ?: density
            CompositionLocalProvider(LocalDensity provides scaled) {
                KronoTheme {
                    val sizeModifier = Modifier
                        .then(widthDp?.let { Modifier.width(it) } ?: Modifier)
                        .then(heightDp?.let { Modifier.height(it) } ?: Modifier)
                    CalendarRoute(onOpenTask = { openedTasks += it }, modifier = sizeModifier, viewModel = viewModel)
                }
            }
        }
    }

    private fun givenDesignTasks() {
        repository.givenTasks(
            Task(
                id = "clase",
                title = CLASS_TITLE,
                category = TaskCategory.ESTUDIO,
                dueDate = LocalDateTime(2024, 10, 24, 16, 30),
                estimatedDuration = 90.minutes,
            ),
            Task(
                id = "soporte",
                title = SUPPORT_TITLE,
                category = TaskCategory.TRABAJO,
                dueDate = LocalDateTime(2024, 10, 24, 15, 0),
                estimatedDuration = 2.hours,
            ),
            Task(id = "informe", title = REPORT_TITLE, dueDate = LocalDateTime(2024, 10, 29, 10, 0)),
        )
    }

    /** Celda del día cuya descripción hablada empieza por [spokenDate]. */
    private fun dayCell(spokenDate: String) =
        composeRule.onNode(hasContentDescription(spokenDate, substring = true) and isSelectable())

    /** Las celdas de día: seleccionables con una fecha hablada ("... de octubre de 2024"). */
    private fun allDayCells() = composeRule.onAllNodes(isSelectable() and hasContentDescription(" de ", substring = true))

    private fun scrollTo(textToFind: String) {
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText(textToFind))
    }

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

        composeRule.onNodeWithContentDescription(text(R.string.calendar_next_month))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
            .performClick()
        composeRule.onNodeWithText("Noviembre 2024").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.calendar_previous_month))
            .assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
            .performClick()
            .performClick()
        composeRule.onNodeWithText("Septiembre 2024").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(text(R.string.calendar_today_description)).performClick()
        composeRule.onNodeWithText("Octubre 2024").assertIsDisplayed()
    }


    @Test
    fun aAnchoDe360dp_cadaCeldaDeDiaMideAlMenos48dp() = assertEveryDayCellHasMinTouchTarget(fontScale = null)

    @Test
    fun aAnchoDe360dpYFuenteAl130PorCiento_cadaCeldaDeDiaMideAlMenos48dp() =
        assertEveryDayCellHasMinTouchTarget(fontScale = LARGE_FONT_SCALE)

    @Test
    fun aAnchoDe360dpYFuenteAl200PorCiento_cadaCeldaDeDiaMideAlMenos48dp() =
        assertEveryDayCellHasMinTouchTarget(fontScale = MAX_FONT_SCALE)

    private fun assertEveryDayCellHasMinTouchTarget(fontScale: Float?) {
        show(fontScale = fontScale, widthDp = NARROW_SCREEN_WIDTH)

        val cells = allDayCells()
        assertEquals(DAYS_IN_FIVE_WEEKS, cells.fetchSemanticsNodes().size)
        repeat(DAYS_IN_FIVE_WEEKS) { index ->
            cells[index].assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

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
    fun soloLaVistaMesEstaActivaYSemanaYDiaSeAnuncianComoProximamente() {
        show()

        composeRule.onNodeWithText(text(R.string.calendar_view_month)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.calendar_view_week)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.calendar_view_day)).assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.calendar_views_coming_soon_note)).assertIsDisplayed()
    }


    @Test
    fun semanaYDia_exponenProximamenteEnSuDescripcionYEstanDeshabilitadas() {
        show()

        listOf(R.string.calendar_view_week, R.string.calendar_view_day).forEach { viewName ->
            val description = text(R.string.calendar_view_unavailable_description, text(viewName), text(R.string.calendar_view_coming_soon))
            composeRule.onNodeWithContentDescription(description).assertIsNotEnabled()
        }
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
        composeRule.onNodeWithContentDescription(text(R.string.calendar_previous_month)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.calendar_today_description)).performClick()

        listOf("Octubre 2024", text(R.string.calendar_views_coming_soon_note), "24", "Jueves 24").forEach(::assertTextFits)
        scrollTo(CLASS_TITLE)
        listOf(CLASS_TITLE, "15:00 - 16:30", text(R.string.calendar_status_in_progress)).forEach(::assertTextFits)
    }

    @Test
    fun conFuenteAl200PorCiento_elTituloDeSeptiembreNoPartePalabras() {
        show(fontScale = MAX_FONT_SCALE)

        composeRule.onNodeWithContentDescription(text(R.string.calendar_previous_month)).performClick()

        assertTextFits("Septiembre 2024")
    }

    /** Comprueba que [value] no se corta en vertical y que solo salta de línea entre palabras. */
    private fun assertTextFits(value: String) {
        val nodes = composeRule.onAllNodesWithText(value, useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("No se encontró '$value'", nodes.isNotEmpty())
        nodes.forEach { node ->
            val layouts = mutableListOf<TextLayoutResult>()
            node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            val layout = layouts.first()
            assertTrue(
                "'$value' se recorta: necesita ${layout.multiParagraph.height}px y tiene ${layout.size.height}px",
                ceil(layout.multiParagraph.height) <= layout.size.height,
            )
            (0 until layout.lineCount - 1).forEach { line ->
                val breakIndex = layout.getLineEnd(line)
                assertTrue("'$value' parte una palabra en la línea $line", value[breakIndex - 1].isWhitespace())
            }
        }
    }

    private object FixedClock : Clock {
        override fun now(): Instant = LocalDateTime(2024, 10, 24, 14, 0).toInstant(TimeZone.UTC)
    }

    private companion object {
        const val CLASS_TITLE = "Clase de Álgebra Lineal"
        const val SUPPORT_TITLE = "Trabajo Soporte KRONO"
        const val REPORT_TITLE = "Entrega del informe"
        const val TECHNICAL_ERROR = "PERMISSION_DENIED: Missing or insufficient permissions."
        const val DAYS_IN_FIVE_WEEKS = 35
        const val LARGE_FONT_SCALE = 1.3f
        const val MAX_FONT_SCALE = 2f
        val NARROW_SCREEN_WIDTH = 360.dp
        val LANDSCAPE_WIDTH = 640.dp
        val LANDSCAPE_HEIGHT = 320.dp
        const val LAST_GRID_DAY = "domingo, 3 de noviembre de 2024"
        val MIN_TOUCH_TARGET = 48.dp
    }
}

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
import com.krono.feature.calendar.CalendarViewModel
import com.krono.feature.calendar.R
import com.krono.feature.calendar.month.ui.CalendarRoute
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import kotlin.math.ceil
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Base de las pruebas de UI del calendario: monta [CalendarRoute] con el ViewModel real sobre un
 * repositorio en memoria y la hora fijada el jueves 24 de octubre de 2024 a las 14:00, y reúne los
 * ayudantes comunes (textos, celdas de día, scroll y comprobación de que el texto no se recorta).
 */
abstract class CalendarScreenTestBase {

    @get:Rule
    val composeRule = createComposeRule()

    protected val repository = FakeTaskRepository()
    protected val openedTasks = mutableListOf<String>()

    protected fun text(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    protected fun show(fontScale: Float? = null, widthDp: Dp? = null, heightDp: Dp? = null) {
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

    /** Tareas del diseño: una en curso a las 14:00, otra más tarde ese día y una del martes siguiente. */
    protected fun givenDesignTasks() {
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

    /** Pulsa la pestaña del selector de vista con el texto [labelRes]. */
    protected fun selectView(@StringRes labelRes: Int) {
        composeRule.onNode(hasText(text(labelRes)) and isSelectable()).performClick()
    }

    /** Celda de día cuya descripción hablada empieza por [spokenDate]. */
    protected fun dayCell(spokenDate: String) =
        composeRule.onNode(hasContentDescription(spokenDate, substring = true) and isSelectable())

    /** Las celdas de día: seleccionables con una fecha hablada ("... de octubre de 2024"). */
    protected fun allDayCells() =
        composeRule.onAllNodes(isSelectable() and hasContentDescription(" de ", substring = true))

    protected fun scrollTo(textToFind: String) {
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
            .performScrollToNode(hasText(textToFind))
    }

    protected fun clickButton(@StringRes descriptionRes: Int) {
        composeRule.onNodeWithContentDescription(text(descriptionRes)).performClick()
    }

    /** Comprueba que cada celda de día mide al menos 48 dp de ancho y de alto, en la vista [view] si se indica. */
    protected fun assertEveryDayCellHasMinTouchTarget(
        expectedCells: Int,
        fontScale: Float?,
        @StringRes view: Int? = null,
    ) {
        show(fontScale = fontScale, widthDp = NARROW_SCREEN_WIDTH)
        view?.let(::selectView)

        val cells = allDayCells()
        assertEquals(expectedCells, cells.fetchSemanticsNodes().size)
        repeat(expectedCells) { index ->
            cells[index].assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    /** Comprueba que los botones de navegación y "Hoy" miden al menos 48 dp. */
    protected fun assertNavigationButtonsHaveMinTouchTarget(@StringRes previous: Int, @StringRes next: Int) {
        listOf(previous, next, R.string.calendar_today_description).forEach { description ->
            composeRule.onNodeWithContentDescription(text(description))
                .assertWidthIsAtLeast(MIN_TOUCH_TARGET).assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    /** Comprueba que [value] no se corta en vertical y que solo salta de línea entre palabras. */
    protected fun assertTextFits(value: String) {
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

    protected companion object {
        const val CLASS_TITLE = "Clase de Álgebra Lineal"
        const val SUPPORT_TITLE = "Trabajo Soporte KRONO"
        const val REPORT_TITLE = "Entrega del informe"
        const val TECHNICAL_ERROR = "PERMISSION_DENIED: Missing or insufficient permissions."
        const val LARGE_FONT_SCALE = 1.3f
        const val MAX_FONT_SCALE = 2f
        val NARROW_SCREEN_WIDTH = 360.dp
        val LANDSCAPE_WIDTH = 640.dp
        val LANDSCAPE_HEIGHT = 320.dp
        val MIN_TOUCH_TARGET = 48.dp
    }
}

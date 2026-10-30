package com.krono.feature.dashboard.ui

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoTheme
import com.krono.feature.dashboard.DashboardUiState
import com.krono.feature.dashboard.PriorityTaskUi
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de UI de [DashboardScreen] (F-13 CA-1, CA-2 y CA-3): contenido, estados y acciones. */
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tasks = listOf(
        PriorityTaskUi("t1", "Revisión de diseño", LocalTime(10, 30), TaskCategory.TRABAJO, TaskPriority.ALTA),
        PriorityTaskUi("t2", "Planificar semana", LocalTime(16, 0), TaskCategory.PERSONAL, TaskPriority.MEDIA),
    )

    private fun content(
        userName: String? = "Alexa",
        hasTasksToday: Boolean = true,
        priorityTasks: List<PriorityTaskUi> = tasks,
    ) = DashboardUiState.Content(
        userName = userName,
        completedPercent = 75,
        completedCount = 6,
        pendingCount = 2,
        focusTime = 90.minutes,
        hasTasksToday = hasTasksToday,
        priorityTasks = priorityTasks,
    )

    private fun show(state: DashboardUiState, actions: DashboardActions = DashboardActions()) {
        composeRule.setContent { KronoTheme { DashboardScreen(state = state, actions = actions) } }
    }

    @Test
    fun conContenido_muestraSaludoResumenYTareasPrioritarias() {
        show(content())

        composeRule.onNodeWithText("Hola, Alexa").assertIsDisplayed()
        composeRule.onNodeWithText("75% completado").assertIsDisplayed()
        composeRule.onNodeWithText("6").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("1 h 30 min").assertIsDisplayed()
        composeRule.onNodeWithText("Revisión de diseño").assertIsDisplayed()
        composeRule.onNodeWithText("10:30 • Trabajo").assertIsDisplayed()
    }

    @Test
    fun sinNombre_saludaSinNombre() {
        show(content(userName = null))

        composeRule.onNodeWithText("Hola").assertIsDisplayed()
    }

    @Test
    fun cargando_anunciaLaCarga() {
        show(DashboardUiState.Loading)

        composeRule.onNodeWithContentDescription("Cargando tu día").assertIsDisplayed()
    }

    @Test
    fun sinTareasHoy_muestraElEstadoVacio() {
        show(content(hasTasksToday = false, priorityTasks = emptyList()))

        composeRule.onNodeWithText("No tienes tareas para hoy").assertIsDisplayed()
    }

    @Test
    fun conTodoCompletado_muestraElMensajeDeLogro() {
        show(content(priorityTasks = emptyList()))

        composeRule.onNodeWithText("Completaste todas las tareas de hoy").assertIsDisplayed()
    }

    @Test
    fun enError_muestraLaTarjetaYReintentarLlamaALaAccion() {
        var retries = 0
        show(DashboardUiState.Error, DashboardActions(onRetry = { retries++ }))

        composeRule.onNodeWithText("No pudimos cargar tu día").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(1, retries)
    }

    @Test
    fun iniciar_arrancaElEnfoqueConLaTareaMasPrioritaria() {
        var started: List<String?> = emptyList()
        show(content(), DashboardActions(onStartFocus = { started = started + it }))

        composeRule.onNodeWithText("Iniciar").performClick()

        assertEquals(listOf<String?>("t1"), started)
    }

    @Test
    fun iniciarSinTareas_arrancaElEnfoqueSinTarea() {
        var started: List<String?> = listOf("marcador")
        show(
            content(hasTasksToday = false, priorityTasks = emptyList()),
            DashboardActions(onStartFocus = { started = listOf(it) }),
        )

        composeRule.onNodeWithText("Iniciar").performClick()

        assertEquals(listOf<String?>(null), started)
    }

    @Test
    fun verTodas_llamaALaAccion() {
        var seeAll = 0
        show(content(), DashboardActions(onSeeAll = { seeAll++ }))

        composeRule.onNodeWithText("Ver todas").performClick()

        assertEquals(1, seeAll)
    }

    @Test
    fun tocarUnaTarea_abreSuDetalle() {
        var opened: String? = null
        show(content(), DashboardActions(onOpenTask = { opened = it }))

        composeRule.onNodeWithText("Planificar semana").performClick()

        assertEquals("t2", opened)
    }

    @Test
    fun losControlesTactilesMidenAlMenos48dp() {
        show(content())

        composeRule.onNodeWithText("Iniciar").assertHeightIsAtLeast(KronoSizes.minTouchTarget)
        composeRule.onNodeWithText("Ver todas").assertHeightIsAtLeast(KronoSizes.minTouchTarget)
        composeRule.onNodeWithText("Revisión de diseño").assertHeightIsAtLeast(KronoSizes.minTouchTarget)
    }
}

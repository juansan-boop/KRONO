package com.krono.core.domain.assistant

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.DeleteTaskUseCase
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [EstimateTaskDurationUseCase] (F-11 CA-1 a CA-3 y Modelo Híbrido H1). */
class EstimateTaskDurationUseCaseTest {

    private val estimate = EstimateTaskDurationUseCase()

    @Test
    fun conHistorialVacio_noHaySugerencia() {
        assertNull(estimate(TaskCategory.ESTUDIO, emptyList()))
    }

    @Test
    fun conMenosDeTresTareasMedidas_noHaySugerencia() {
        val tasks = listOf(
            completed("1", TaskCategory.ESTUDIO, 30.minutes, day = 1),
            completed("2", TaskCategory.ESTUDIO, 40.minutes, day = 2),
        )

        assertNull(estimate(TaskCategory.ESTUDIO, tasks))
    }

    @Test
    fun conTresTareasMedidas_promediaYRedondeaACincoMinutos() {
        val tasks = listOf(
            completed("1", TaskCategory.ESTUDIO, 30.minutes, day = 1),
            completed("2", TaskCategory.ESTUDIO, 40.minutes, day = 2),
            completed("3", TaskCategory.ESTUDIO, 41.minutes, day = 3),
        )

        // Promedio 37 min → múltiplo de 5 más cercano: 35 min.
        assertEquals(35.minutes, estimate(TaskCategory.ESTUDIO, tasks))
    }

    @Test
    fun soloUsaLasCincoTareasMasRecientes() {
        val tasks = (1..5).map { completed("$it", TaskCategory.TRABAJO, 20.minutes, day = it + 10) } +
            completed("antigua", TaskCategory.TRABAJO, 300.minutes, day = 1)

        assertEquals(20.minutes, estimate(TaskCategory.TRABAJO, tasks))
    }

    @Test
    fun ignoraOtrasCategoriasPendientesYSinTiempoMedido() {
        val tasks = listOf(
            completed("1", TaskCategory.PERSONAL, 10.minutes, day = 1),
            completed("2", TaskCategory.PERSONAL, 10.minutes, day = 2),
            completed("otra", TaskCategory.ESTUDIO, 90.minutes, day = 3),
            completed("sin-medir", TaskCategory.PERSONAL, Duration.ZERO, day = 4),
            completed("pendiente", TaskCategory.PERSONAL, 50.minutes, day = 5).copy(isCompleted = false),
        )

        assertNull(estimate(TaskCategory.PERSONAL, tasks))
    }

    @Test
    fun unPromedioMuyCorto_seRedondeaAlMenosACincoMinutos() {
        val tasks = (1..3).map { completed("$it", TaskCategory.PERSONAL, 1.minutes, day = it) }

        assertEquals(5.minutes, estimate(TaskCategory.PERSONAL, tasks))
    }

    // --- Pruebas del Modelo Híbrido H1 (predictHybrid) ---

    @Test
    fun predictHybrid_conBaseEstimate_aplicaM1SinArranqueFrio() {
        // Estimación base 30 min → M1 en la tarde con prioridad MEDIA da ~41.6 min → redondea a 40 min
        val pred = estimate.predictHybrid(
            category = TaskCategory.ESTUDIO,
            tasks = emptyList(),
            baseEstimate = 30.minutes,
            priority = TaskPriority.MEDIA,
            daysUntilDue = 2.0,
            hourOfDay = 14,
        )

        assertEquals(40.minutes, pred)
    }

    @Test
    fun predictHybrid_sinBaseEstimateYSinHistorial_retornaNull() {
        val pred = estimate.predictHybrid(
            category = TaskCategory.ESTUDIO,
            tasks = emptyList(),
            baseEstimate = null,
        )

        assertNull(pred)
    }

    @Test
    fun predictHybrid_sinBaseEstimatePeroConHistorial_usaBaseHistorica() {
        val tasks = listOf(
            completed("1", TaskCategory.ESTUDIO, 30.minutes, day = 1),
            completed("2", TaskCategory.ESTUDIO, 30.minutes, day = 2),
            completed("3", TaskCategory.ESTUDIO, 30.minutes, day = 3),
        )

        // Historial base da 30 min → M1 expande a 40 min
        val pred = estimate.predictHybrid(
            category = TaskCategory.ESTUDIO,
            tasks = tasks,
            baseEstimate = null,
            priority = TaskPriority.MEDIA,
            daysUntilDue = 2.0,
            hourOfDay = 14,
        )

        assertEquals(40.minutes, pred)
    }

    @Test
    fun predictHybrid_conHistorialDeSesgo_adaptaOnlineHaciaArriba() {
        // Tareas con estimación 30 min pero duración real de 65 min (subestimación severa)
        val tasks = (1..3).map {
            Task(
                id = "$it",
                title = "Tarea $it",
                category = TaskCategory.TRABAJO,
                estimatedDuration = 30.minutes,
                actualDuration = 65.minutes,
                dueDate = LocalDateTime(2026, 10, it, 14, 0),
                isCompleted = true,
            )
        }

        val predSinHistorial = estimate.predictHybrid(
            category = TaskCategory.TRABAJO,
            tasks = emptyList(),
            baseEstimate = 30.minutes,
        )!!

        val predConHistorial = estimate.predictHybrid(
            category = TaskCategory.TRABAJO,
            tasks = tasks,
            baseEstimate = 30.minutes,
        )!!

        // La adaptación online por sesgo personal incrementa la predicción
        assertTrue(predConHistorial > predSinHistorial)
    }

    @Test
    fun unaTareaEliminada_dejaDeContarParaLaEstimacion() = runTest {
        // F-07 CA-4: la estimación lee el historial de las tareas que existen, así que el tiempo
        // de una tarea borrada (y sus sesiones) deja de contar aunque las sesiones sigan guardadas.
        val repository = FakeTaskRepository().apply {
            givenTasks(
                completed("1", TaskCategory.ESTUDIO, 30.minutes, day = 1),
                completed("2", TaskCategory.ESTUDIO, 30.minutes, day = 2),
                completed("borrada", TaskCategory.ESTUDIO, 300.minutes, day = 3),
                completed("4", TaskCategory.ESTUDIO, 30.minutes, day = 4),
            )
        }

        DeleteTaskUseCase(repository)("borrada")
        val history = (repository.observeTasks().first() as KronoResult.Success).data

        assertEquals(30.minutes, estimate(TaskCategory.ESTUDIO, history))
    }

    @Test
    fun predictHybrid_ignoraTareasEliminadasEnElCalculoDeSesgo() {
        val tasks = listOf(
            Task(
                id = "1",
                title = "Tarea 1",
                category = TaskCategory.TRABAJO,
                estimatedDuration = 30.minutes,
                actualDuration = 60.minutes,
                dueDate = LocalDateTime(2026, 10, 1, 14, 0),
                isCompleted = true,
                isDeleted = false,
            ),
            Task(
                id = "eliminada",
                title = "Tarea eliminada con sesgo irreal",
                category = TaskCategory.TRABAJO,
                estimatedDuration = 10.minutes,
                actualDuration = 120.minutes,
                dueDate = LocalDateTime(2026, 10, 2, 14, 0),
                isCompleted = true,
                isDeleted = true,
            ),
        )

        val predConEliminada = estimate.predictHybrid(
            category = TaskCategory.TRABAJO,
            tasks = tasks,
            baseEstimate = 30.minutes,
        )

        val predSoloActivas = estimate.predictHybrid(
            category = TaskCategory.TRABAJO,
            tasks = listOf(tasks.first()),
            baseEstimate = 30.minutes,
        )

        assertEquals(predSoloActivas, predConEliminada)
    }

    private fun completed(id: String, category: TaskCategory, actual: Duration, day: Int) = Task(
        id = id,
        title = "Tarea $id",
        category = category,
        dueDate = LocalDateTime(2026, 10, day, 12, 0),
        actualDuration = actual,
        isCompleted = true,
    )
}

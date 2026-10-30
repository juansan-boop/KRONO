package com.krono.core.domain.assistant

import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import com.krono.core.domain.time.plus
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Pruebas de [BuildPriorityPlanUseCase] y [PriorityScoring] (F-12 CA-1). */
class BuildPriorityPlanUseCaseTest {

    private val now = LocalDateTime(2026, 10, 22, 14, 0)
    private val buildPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase())

    @Test
    fun sinTareasPendientes_noHayPlan() {
        val completed = task("1", dueIn = 2.hours).copy(isCompleted = true)

        assertNull(buildPlan(listOf(completed), now))
    }

    @Test
    fun recomiendaLaTareaMasUrgente() {
        val nextWeek = task("semana", dueIn = 100.hours, priority = TaskPriority.ALTA)
        val inFourHours = task("hoy", dueIn = 4.hours, priority = TaskPriority.MEDIA)

        val plan = buildPlan(listOf(nextWeek, inFourHours), now)!!

        assertEquals("hoy", plan.recommended.task.id)
        assertTrue(PriorityFactor.DUE_VERY_SOON in plan.recommended.factors)
        assertEquals(listOf("semana"), plan.sequence.map { it.task.id })
    }

    @Test
    fun aIgualUrgencia_ganaLaPrioridadAlta() {
        val low = task("baja", dueIn = 20.hours, priority = TaskPriority.BAJA)
        val high = task("alta", dueIn = 20.hours, priority = TaskPriority.ALTA)

        val plan = buildPlan(listOf(low, high), now)!!

        assertEquals("alta", plan.recommended.task.id)
        assertTrue(PriorityFactor.HIGH_PRIORITY in plan.recommended.factors)
    }

    @Test
    fun unaTareaVencida_vaPrimeroYNoCuentaComoMargenAjustado() {
        val overdue = task("vencida", dueIn = (-1).hours, priority = TaskPriority.BAJA)
        val urgent = task("urgente", dueIn = 2.hours, priority = TaskPriority.ALTA)

        val plan = buildPlan(listOf(urgent, overdue), now)!!

        assertEquals("vencida", plan.recommended.task.id)
        assertEquals(setOf(PriorityFactor.OVERDUE), plan.recommended.factors)
    }

    @Test
    fun siLaEstimacionOcupaMedioMargen_marcaTiempoAjustado() {
        val tight = task("ajustada", dueIn = 2.hours, estimate = 60.minutes)

        val plan = buildPlan(listOf(tight), now)!!

        assertTrue(PriorityFactor.TIGHT_SCHEDULE in plan.recommended.factors)
    }

    @Test
    fun encadenaLosBloquesDesdeAhora() {
        val first = task("a", dueIn = 3.hours, estimate = 45.minutes, priority = TaskPriority.ALTA)
        val second = task("b", dueIn = 30.hours, estimate = 30.minutes)

        val plan = buildPlan(listOf(second, first), now)!!

        assertEquals(now, plan.recommended.start)
        assertEquals(LocalDateTime(2026, 10, 22, 14, 45), plan.recommended.end)
        assertEquals(LocalDateTime(2026, 10, 22, 14, 45), plan.sequence.single().start)
        assertEquals(LocalDateTime(2026, 10, 22, 15, 15), plan.sequence.single().end)
    }

    @Test
    fun sinEstimacionManual_usaHistorialYLuegoElValorPorDefecto() {
        val history = (1..3).map {
            task("h$it", dueIn = (-it * 24).hours, category = TaskCategory.ESTUDIO)
                .copy(isCompleted = true, actualDuration = 40.minutes)
        }
        val study = task("estudio", dueIn = 5.hours, category = TaskCategory.ESTUDIO)
        val personal = task("personal", dueIn = 50.hours, category = TaskCategory.PERSONAL)

        val plan = buildPlan(history + study + personal, now)!!

        assertEquals(EstimateSource.HISTORY, plan.recommended.estimateSource)
        assertEquals(40.minutes, plan.recommended.estimate)
        assertEquals(EstimateSource.DEFAULT, plan.sequence.single().estimateSource)
        assertEquals(BuildPriorityPlanUseCase.DEFAULT_ESTIMATE_MINUTES.minutes, plan.sequence.single().estimate)
    }

    @Test
    fun laSecuenciaSeLimitaAlMaximoDelPlan() {
        val tasks = (1..8).map { task("$it", dueIn = (it * 10).hours) }

        val plan = buildPlan(tasks, now)!!

        assertEquals(BuildPriorityPlanUseCase.MAX_PLANNED_TASKS - 1, plan.sequence.size)
    }

    @Test
    fun laConfianzaSubeCuandoLaRecomendacionSeDestaca() {
        val tie = buildPlan(listOf(task("a", dueIn = 30.hours), task("b", dueIn = 30.hours)), now)!!
        val clear = buildPlan(
            listOf(
                task("a", dueIn = 2.hours, priority = TaskPriority.ALTA, estimate = 30.minutes),
                task("b", dueIn = 200.hours, priority = TaskPriority.BAJA),
            ),
            now,
        )!!

        assertTrue(clear.confidencePercent > tie.confidencePercent)
        assertTrue(clear.confidencePercent <= BuildPriorityPlanUseCase.MAX_CONFIDENCE)
        assertTrue(tie.confidencePercent >= BuildPriorityPlanUseCase.BASE_CONFIDENCE)
    }

    @Test
    fun elBalancePorCategoriaSuma100() {
        val tasks = listOf(
            task("e", dueIn = 5.hours, category = TaskCategory.ESTUDIO, estimate = 20.minutes),
            task("t", dueIn = 6.hours, category = TaskCategory.TRABAJO, estimate = 20.minutes),
            task("p", dueIn = 7.hours, category = TaskCategory.PERSONAL, estimate = 20.minutes),
        )

        val balance = buildPlan(tasks, now)!!.categoryBalance

        assertEquals(100, balance.values.sum())
        assertEquals(TaskCategory.entries.toSet(), balance.keys)
    }

    @Test
    fun unaTareaSinFecha_noTieneFactoresDeUrgencia() {
        val plan = buildPlan(listOf(task("libre", dueIn = null)), now)!!

        assertNull(plan.recommended.timeUntilDue)
        assertTrue(plan.recommended.factors.isEmpty())
    }

    private fun task(
        id: String,
        dueIn: Duration?,
        priority: TaskPriority = TaskPriority.MEDIA,
        category: TaskCategory = TaskCategory.TRABAJO,
        estimate: Duration? = null,
    ) = Task(
        id = id,
        title = "Tarea $id",
        category = category,
        priority = priority,
        dueDate = dueIn?.let { now + it },
        estimatedDuration = estimate,
    )
}

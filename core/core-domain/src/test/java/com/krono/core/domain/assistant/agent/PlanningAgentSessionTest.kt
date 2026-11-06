package com.krono.core.domain.assistant.agent

import com.krono.core.common.KronoResult
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateSource
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskCategory
import com.krono.core.domain.task.TaskPriority
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

/** Pruebas de las herramientas del agente de planificación (F-28). */
class PlanningAgentSessionTest {

    private val now = LocalDateTime(2026, 11, 6, 14, 0)
    private val buildPlan = BuildPriorityPlanUseCase(EstimateTaskDurationUseCase())

    @Test
    fun createTask_asignaAliasCortoEIdDefinitivo() {
        val session = session()

        val draft = session.createTask(request("Estudiar para el parcial")).successData()

        assertEquals("t1", draft.alias)
        assertEquals("id-1", draft.id)
        assertNull(draft.estimate)
    }

    @Test
    fun createTask_conElMismoTitulo_noDuplicaElBorrador() {
        val session = session()
        session.createTask(request("Estudiar para el parcial"))

        val repeated = session.createTask(request("  estudiar PARA el parcial ")).successData()

        assertEquals("t1", repeated.alias)
        assertEquals(1, session.drafts.size)
    }

    @Test
    fun createTask_conFechaPasada_devuelveBorradorInvalido() {
        val result = session().createTask(request("Informe", due = LocalDateTime(2026, 11, 5, 9, 0)))

        assertTrue((result as KronoResult.Error).throwable is PlanningAgentError.InvalidDraft)
    }

    @Test
    fun createTask_conDuracionFueraDeRango_devuelveBorradorInvalido() {
        val result = session().createTask(request("Informe", userMinutes = 2))

        assertTrue((result as KronoResult.Error).throwable is PlanningAgentError.InvalidDraft)
    }

    @Test
    fun createTask_masAllaDelMaximo_devuelveDemasiadasTareas() {
        val session = session()
        repeat(PlanningAgentSession.MAX_DRAFTS) { session.createTask(request("Tarea $it")) }

        val result = session.createTask(request("Una más"))

        assertEquals(KronoResult.Error(PlanningAgentError.TooManyDrafts), result)
    }

    @Test
    fun estimateTask_sinHistorial_usaLaDuracionPorDefecto() {
        val session = session()
        session.createTask(request("Leer capítulo 3"))

        val estimate = session.estimateTask("t1").successData().estimate

        assertEquals(DraftEstimate(BuildPriorityPlanUseCase.DEFAULT_ESTIMATE_MINUTES.minutes, EstimateSource.DEFAULT), estimate)
    }

    @Test
    fun estimateTask_conHistorialDeLaCategoria_usaH1() {
        val history = (1..3).map { measured("hecha-$it", minutes = 60) }
        val session = session(currentTasks = history)
        session.createTask(request("Estudiar para el parcial"))

        val estimate = session.estimateTask("t1").successData().estimate

        assertEquals(DraftEstimate(60.minutes, EstimateSource.HISTORY), estimate)
    }

    @Test
    fun estimateTask_conAliasInexistente_devuelveNoEncontrado() {
        val result = session().estimateTask("t9")

        assertEquals(KronoResult.Error(PlanningAgentError.DraftNotFound("t9")), result)
    }

    @Test
    fun toProposal_estimaLosBorradoresAunqueElModeloNoLoHayaPedido() {
        val session = session()
        session.createTask(request("Leer capítulo 3"))

        val proposal = session.toProposal("  Te propongo una tarea.  ")

        assertEquals(EstimateSource.DEFAULT, proposal.drafts.single().estimate?.source)
        assertEquals("Te propongo una tarea.", proposal.message)
    }

    @Test
    fun elOrdenDelPlanLoDecideH2_noElOrdenEnQueElModeloCreoLasTareas() {
        val session = session()
        session.createTask(request("Ordenar el escritorio", priority = TaskPriority.BAJA, category = TaskCategory.PERSONAL))
        session.createTask(request("Estudiar para el parcial", priority = TaskPriority.ALTA, due = LocalDateTime(2026, 11, 6, 19, 0)))

        val plan = session.toProposal(null).plan

        assertEquals("Estudiar para el parcial", plan?.recommended?.task?.title)
        assertEquals(listOf("Ordenar el escritorio"), plan?.sequence?.map { it.task.title })
    }

    @Test
    fun elPlanIncluyeLasTareasActualesYLasPropuestas() {
        val existing = Task(id = "informe", title = "Informe de laboratorio", dueDate = LocalDateTime(2026, 11, 7, 12, 0))
        val session = session(currentTasks = listOf(existing))
        session.createTask(request("Estudiar para el parcial", due = LocalDateTime(2026, 11, 12, 7, 0)))

        val plan = session.buildPlan()
        val planned = listOfNotNull(plan?.recommended) + plan?.sequence.orEmpty()

        assertEquals(setOf("informe", "id-1"), planned.map { it.task.id }.toSet())
    }

    private fun session(currentTasks: List<Task> = emptyList()): PlanningAgentSession {
        var next = 0
        return PlanningAgentSession(currentTasks, now, buildPlan) { "id-${++next}" }
    }

    private fun request(
        title: String,
        category: TaskCategory = TaskCategory.ESTUDIO,
        priority: TaskPriority = TaskPriority.MEDIA,
        due: LocalDateTime? = null,
        userMinutes: Int? = null,
    ) = TaskDraftRequest(title, category, priority, due, userMinutes?.minutes)

    private fun measured(id: String, minutes: Int) = Task(
        id = id,
        title = id,
        category = TaskCategory.ESTUDIO,
        actualDuration = minutes.minutes,
        isCompleted = true,
        dueDate = LocalDateTime(2026, 11, 1, 10, 0),
    )

    private fun <T> KronoResult<T>.successData(): T = (this as KronoResult.Success<T>).data
}

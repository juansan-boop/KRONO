package com.krono.feature.calendar.month

import com.krono.core.common.KronoResult
import com.krono.core.domain.task.FakeTaskRepository
import com.krono.core.domain.task.Task
import com.krono.core.domain.task.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/** Reloj fijo y modificable: las pruebas eligen la hora "actual" en UTC. */
class FixedClock(var current: LocalDateTime) : Clock {
    override fun now(): Instant = current.toInstant(TimeZone.UTC)
}

/**
 * Repositorio cuya observación de tareas controla la prueba: no emite nada hasta que se llama a
 * [emit], para poder ver el estado de carga. El resto de operaciones delega en un
 * [FakeTaskRepository].
 */
class ControlledTaskRepository(
    private val delegate: FakeTaskRepository = FakeTaskRepository(),
) : TaskRepository by delegate {

    private val results = MutableSharedFlow<KronoResult<List<Task>>>(replay = 1)

    /** Emite [result] a quien esté observando las tareas. */
    suspend fun emit(result: KronoResult<List<Task>>) = results.emit(result)

    override fun observeTasks(): Flow<KronoResult<List<Task>>> = results
}

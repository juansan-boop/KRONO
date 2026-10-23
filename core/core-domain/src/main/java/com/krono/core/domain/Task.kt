package com.krono.core.domain

import com.krono.core.domain.task.Subtask as DomainSubtask
import com.krono.core.domain.task.Task as DomainTask
import com.krono.core.domain.task.TaskCategory as DomainTaskCategory
import com.krono.core.domain.task.TaskPriority as DomainTaskPriority

/** Alias para mantener compatibilidad con imports directos a nivel raíz de dominio. */
typealias Task = DomainTask
typealias Subtask = DomainSubtask
typealias TaskCategory = DomainTaskCategory
typealias TaskPriority = DomainTaskPriority

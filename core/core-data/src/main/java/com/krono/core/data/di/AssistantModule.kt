package com.krono.core.data.di

import com.krono.core.data.assistant.FirebaseAiPriorityExplanationGenerator
import com.krono.core.data.assistant.agent.FirebaseAiPlanningAgent
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.ExplainPriorityUseCase
import com.krono.core.domain.assistant.PriorityExplanationGenerator
import com.krono.core.domain.assistant.agent.AskPlanningAgentUseCase
import com.krono.core.domain.assistant.agent.ConfirmAgentProposalUseCase
import com.krono.core.domain.assistant.agent.PlanningAgent
import com.krono.core.domain.task.UpsertTaskUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Enlaza el redactor de explicaciones (D-5) y el agente de planificación (F-28) con sus
 * implementaciones en Firebase AI Logic.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantBindingsModule {

    /** Redactor de explicaciones con Gemini. */
    @Binds
    @Singleton
    abstract fun bindPriorityExplanationGenerator(
        impl: FirebaseAiPriorityExplanationGenerator,
    ): PriorityExplanationGenerator

    /** Agente de planificación con function calling de Gemini (F-28). */
    @Binds
    @Singleton
    abstract fun bindPlanningAgent(impl: FirebaseAiPlanningAgent): PlanningAgent
}

/** Provee los casos de uso de estimación (F-11), del asistente de prioridades (F-12) y del agente (F-28). */
@Module
@InstallIn(SingletonComponent::class)
object AssistantModule {

    /** Caso de uso de estimación por historial (F-11). */
    @Provides
    fun provideEstimateTaskDurationUseCase() = EstimateTaskDurationUseCase()

    /** Caso de uso que arma el plan de prioridades (F-12). */
    @Provides
    fun provideBuildPriorityPlanUseCase(estimateTaskDuration: EstimateTaskDurationUseCase) =
        BuildPriorityPlanUseCase(estimateTaskDuration)

    /** Caso de uso que pide la explicación de la tarea recomendada (F-12). */
    @Provides
    fun provideExplainPriorityUseCase(generator: PriorityExplanationGenerator) =
        ExplainPriorityUseCase(generator)

    /** Caso de uso que atiende un mensaje libre con el agente de planificación (F-28). */
    @Provides
    fun provideAskPlanningAgentUseCase(agent: PlanningAgent, buildPriorityPlan: BuildPriorityPlanUseCase) =
        AskPlanningAgentUseCase(agent, buildPriorityPlan)

    /** Caso de uso que guarda las tareas propuestas por el agente al confirmarlas (F-28). */
    @Provides
    fun provideConfirmAgentProposalUseCase(upsertTask: UpsertTaskUseCase) =
        ConfirmAgentProposalUseCase(upsertTask)
}

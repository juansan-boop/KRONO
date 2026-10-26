package com.krono.core.data.di

import com.krono.core.data.assistant.FirebaseAiPriorityExplanationGenerator
import com.krono.core.domain.assistant.BuildPriorityPlanUseCase
import com.krono.core.domain.assistant.EstimateTaskDurationUseCase
import com.krono.core.domain.assistant.ExplainPriorityUseCase
import com.krono.core.domain.assistant.PriorityExplanationGenerator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaza el redactor de explicaciones del asistente con su implementación en Firebase AI Logic (D-5). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantBindingsModule {

    /** Redactor de explicaciones con Gemini. */
    @Binds
    @Singleton
    abstract fun bindPriorityExplanationGenerator(
        impl: FirebaseAiPriorityExplanationGenerator,
    ): PriorityExplanationGenerator
}

/** Provee los casos de uso de estimación (F-11) y del asistente de prioridades (F-12). */
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
}

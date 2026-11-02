package com.krono.core.data.di

import com.krono.core.data.timer.FirestoreTimerDataSource
import com.krono.core.data.timer.FirestoreTimerRepository
import com.krono.core.data.timer.TimerRemoteDataSource
import com.krono.core.domain.task.TaskRepository
import com.krono.core.domain.timer.FinishTimerUseCase
import com.krono.core.domain.timer.StartFocusBreakUseCase
import com.krono.core.domain.timer.StartFocusUseCase
import com.krono.core.domain.timer.ObserveActiveTimerUseCase
import com.krono.core.domain.timer.PauseTimerUseCase
import com.krono.core.domain.timer.ResumeTimerUseCase
import com.krono.core.domain.timer.StartTimerUseCase
import com.krono.core.domain.timer.TimerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock
import javax.inject.Singleton

/** Enlaza los contratos del cronómetro con su implementación en Firestore (F-08). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TimerBindingsModule {

    /** Fuente remota del cronómetro: Cloud Firestore. */
    @Binds
    @Singleton
    abstract fun bindTimerRemoteDataSource(impl: FirestoreTimerDataSource): TimerRemoteDataSource

    /** Repositorio del cronómetro que consumen los casos de uso. */
    @Binds
    @Singleton
    abstract fun bindTimerRepository(impl: FirestoreTimerRepository): TimerRepository
}

/** Provee los casos de uso del cronómetro de `core-domain` (F-08). */
@Module
@InstallIn(SingletonComponent::class)
object TimerModule {

    /** Reloj del sistema; las pruebas usan uno falso. */
    @Provides
    fun provideClock(): Clock = Clock.System

    /** Caso de uso que observa el cronómetro en curso. */
    @Provides
    fun provideObserveActiveTimerUseCase(repository: TimerRepository) = ObserveActiveTimerUseCase(repository)

    /** Caso de uso que finaliza el cronómetro y registra la sesión (CA-2). */
    @Provides
    fun provideFinishTimerUseCase(repository: TimerRepository, clock: Clock) =
        FinishTimerUseCase(repository, clock)

    /** Caso de uso que inicia el cronómetro (CA-1, CA-4). */
    @Provides
    fun provideStartTimerUseCase(
        repository: TimerRepository,
        taskRepository: TaskRepository,
        finishTimer: FinishTimerUseCase,
        clock: Clock,
    ) = StartTimerUseCase(repository, taskRepository, finishTimer, clock)

    /** Caso de uso que pausa el cronómetro (CA-1). */
    @Provides
    fun providePauseTimerUseCase(repository: TimerRepository, clock: Clock) = PauseTimerUseCase(repository, clock)

    /** Caso de uso que reanuda el cronómetro (CA-1). */
    @Provides
    fun provideResumeTimerUseCase(repository: TimerRepository, clock: Clock) = ResumeTimerUseCase(repository, clock)

    /** Caso de uso que inicia un enfoque con la plantilla activa (F-09 CA-1). */
    @Provides
    fun provideStartFocusUseCase(startTimer: StartTimerUseCase) = StartFocusUseCase(startTimer)

    /** Caso de uso que empieza un descanso dentro del enfoque (F-09 CA-2, CA-3). */
    @Provides
    fun provideStartFocusBreakUseCase(repository: TimerRepository, clock: Clock) =
        StartFocusBreakUseCase(repository, clock)
}

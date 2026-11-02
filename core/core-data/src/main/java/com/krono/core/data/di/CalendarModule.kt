package com.krono.core.data.di

import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.calendar.ObserveCalendarUseCase
import com.krono.core.domain.task.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.datetime.Clock

/** Provee los casos de uso del calendario de `core-domain` (F-16). */
@Module
@InstallIn(SingletonComponent::class)
object CalendarModule {

    /** Caso de uso que observa la agenda de un período a partir de las tareas. */
    @Provides
    fun provideObserveCalendarUseCase(repository: TaskRepository) = ObserveCalendarUseCase(repository)

    /** Fuente de "ahora" local del calendario; usa el [Clock] que provee `TimerModule`. */
    @Provides
    fun provideCurrentDateTimeProvider(clock: Clock) = CurrentDateTimeProvider(clock)
}

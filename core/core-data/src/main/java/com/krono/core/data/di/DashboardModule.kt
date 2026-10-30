package com.krono.core.data.di

import com.krono.core.data.profile.FirestoreUserNameDataSource
import com.krono.core.data.profile.FirestoreUserNameRepository
import com.krono.core.data.profile.UserNameRemoteDataSource
import com.krono.core.data.timer.FirestoreTimeSessionDataSource
import com.krono.core.data.timer.FirestoreTimeSessionRepository
import com.krono.core.data.timer.TimeSessionRemoteDataSource
import com.krono.core.domain.calendar.CurrentDateTimeProvider
import com.krono.core.domain.dashboard.ObserveDashboardUseCase
import com.krono.core.domain.profile.ObserveUserNameUseCase
import com.krono.core.domain.profile.UserNameRepository
import com.krono.core.domain.task.TaskRepository
import com.krono.core.domain.timer.TimeSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaza los contratos de lectura que usa el dashboard con su implementación en Firestore (F-13). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardBindingsModule {

    /** Fuente remota de las sesiones de tiempo: Cloud Firestore. */
    @Binds
    @Singleton
    abstract fun bindTimeSessionRemoteDataSource(impl: FirestoreTimeSessionDataSource): TimeSessionRemoteDataSource

    /** Repositorio de lectura de sesiones de tiempo. */
    @Binds
    @Singleton
    abstract fun bindTimeSessionRepository(impl: FirestoreTimeSessionRepository): TimeSessionRepository

    /** Fuente remota del nombre del perfil: Cloud Firestore. */
    @Binds
    @Singleton
    abstract fun bindUserNameRemoteDataSource(impl: FirestoreUserNameDataSource): UserNameRemoteDataSource

    /** Repositorio de lectura del nombre del usuario. */
    @Binds
    @Singleton
    abstract fun bindUserNameRepository(impl: FirestoreUserNameRepository): UserNameRepository
}

/** Provee los casos de uso del dashboard de inicio de `core-domain` (F-13). */
@Module
@InstallIn(SingletonComponent::class)
object DashboardModule {

    /** Caso de uso que observa el resumen del día (tareas de hoy y tiempo de enfoque). */
    @Provides
    fun provideObserveDashboardUseCase(
        taskRepository: TaskRepository,
        sessionRepository: TimeSessionRepository,
        dateTimeProvider: CurrentDateTimeProvider,
    ) = ObserveDashboardUseCase(taskRepository, sessionRepository, dateTimeProvider)

    /** Caso de uso que observa el nombre con el que se saluda al usuario. */
    @Provides
    fun provideObserveUserNameUseCase(repository: UserNameRepository) = ObserveUserNameUseCase(repository)
}

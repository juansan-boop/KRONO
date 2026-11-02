package com.krono.core.data.di

import com.krono.core.data.focus.FirestoreFocusTemplateDataSource
import com.krono.core.data.focus.FirestoreFocusTemplateRepository
import com.krono.core.data.focus.FocusTemplateRemoteDataSource
import com.krono.core.domain.focus.ActivateFocusTemplateUseCase
import com.krono.core.domain.focus.CreateFocusTemplateUseCase
import com.krono.core.domain.focus.DeleteFocusTemplateUseCase
import com.krono.core.domain.focus.EditFocusTemplateUseCase
import com.krono.core.domain.focus.FocusTemplateRepository
import com.krono.core.domain.focus.GetActiveFocusTemplateUseCase
import com.krono.core.domain.focus.ObserveFocusTemplatesUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Enlaza los contratos de las plantillas de enfoque con su implementación en Firestore (F-10). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FocusTemplateBindingsModule {

    /** Fuente remota de las plantillas: Cloud Firestore. */
    @Binds
    @Singleton
    abstract fun bindFocusTemplateRemoteDataSource(impl: FirestoreFocusTemplateDataSource): FocusTemplateRemoteDataSource

    /** Repositorio de plantillas que consumen los casos de uso. */
    @Binds
    @Singleton
    abstract fun bindFocusTemplateRepository(impl: FirestoreFocusTemplateRepository): FocusTemplateRepository
}

/** Provee los casos de uso de las plantillas de enfoque de `core-domain` (F-10). */
@Module
@InstallIn(SingletonComponent::class)
object FocusTemplateModule {

    /** Caso de uso que observa las plantillas y siembra las de por defecto (CA-1). */
    @Provides
    fun provideObserveFocusTemplatesUseCase(repository: FocusTemplateRepository) =
        ObserveFocusTemplatesUseCase(repository)

    /** Caso de uso que crea una plantilla (CA-2). */
    @Provides
    fun provideCreateFocusTemplateUseCase(repository: FocusTemplateRepository) =
        CreateFocusTemplateUseCase(repository)

    /** Caso de uso que edita una plantilla (CA-2). */
    @Provides
    fun provideEditFocusTemplateUseCase(repository: FocusTemplateRepository) =
        EditFocusTemplateUseCase(repository)

    /** Caso de uso que elimina una plantilla (CA-2). */
    @Provides
    fun provideDeleteFocusTemplateUseCase(repository: FocusTemplateRepository) =
        DeleteFocusTemplateUseCase(repository)

    /** Caso de uso que marca una plantilla como activa (CA-2). */
    @Provides
    fun provideActivateFocusTemplateUseCase(repository: FocusTemplateRepository) =
        ActivateFocusTemplateUseCase(repository)

    /** Caso de uso que obtiene la plantilla con la que arranca el modo enfoque (F-09). */
    @Provides
    fun provideGetActiveFocusTemplateUseCase(repository: FocusTemplateRepository) =
        GetActiveFocusTemplateUseCase(repository)
}

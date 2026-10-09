package com.krono.core.data.di

import com.google.firebase.auth.FirebaseAuth
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.auth.FirebaseAuthDataSource
import com.krono.core.data.auth.FirebaseAuthRepository
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.LoginUseCase
import com.krono.core.domain.auth.LogoutUseCase
import com.krono.core.domain.auth.ObserveSessionUseCase
import com.krono.core.domain.auth.RegisterUseCase
import com.krono.core.domain.auth.ResetPasswordUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provee el cliente de Firebase Authentication, que se inicializa con `google-services.json` de `app`. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /** Idioma de los correos que envía Firebase (por ejemplo, el de recuperar contraseña). */
    private const val EMAIL_LANGUAGE_CODE = "es"

    /** Instancia única de Firebase Authentication de la app, con los correos en español. */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth =
        FirebaseAuth.getInstance().apply { setLanguageCode(EMAIL_LANGUAGE_CODE) }
}

/** Enlaza el contrato de autenticación del dominio con su implementación en Firebase (D-1). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    /** Fuente remota de autenticación: Firebase Authentication. */
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: FirebaseAuthDataSource): AuthRemoteDataSource

    /** Repositorio de autenticación que consumen los casos de uso. */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository
}

/**
 * Los casos de uso viven en `core-domain` (Kotlin puro, sin javax.inject), así
 * que se proveen aquí.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /** Caso de uso de iniciar sesión (F-22). */
    @Provides
    fun provideLoginUseCase(repository: AuthRepository) = LoginUseCase(repository)

    /** Caso de uso de crear cuenta (F-23). */
    @Provides
    fun provideRegisterUseCase(repository: AuthRepository) = RegisterUseCase(repository)

    /** Caso de uso de recuperar contraseña (F-24). */
    @Provides
    fun provideResetPasswordUseCase(repository: AuthRepository) = ResetPasswordUseCase(repository)

    /** Caso de uso de cerrar sesión (F-25). */
    @Provides
    fun provideLogoutUseCase(repository: AuthRepository) = LogoutUseCase(repository)

    /** Caso de uso que observa la sesión activa (CA-6). */
    @Provides
    fun provideObserveSessionUseCase(repository: AuthRepository) = ObserveSessionUseCase(repository)
}

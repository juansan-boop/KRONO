package com.krono.core.data.di

import android.content.Context
import androidx.room.Room
import com.krono.core.data.auth.AuthRepositoryImpl
import com.krono.core.data.auth.PasswordHasher
import com.krono.core.data.auth.Pbkdf2PasswordHasher
import com.krono.core.data.auth.local.AuthDao
import com.krono.core.data.local.KronoDatabase
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KronoDatabase =
        Room.databaseBuilder(context, KronoDatabase::class.java, KronoDatabase.NAME).build()

    @Provides
    fun provideAuthDao(database: KronoDatabase): AuthDao = database.authDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthBindingsModule {

    /** Punto único a cambiar cuando exista el backend (D-1). */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}

/**
 * Los casos de uso viven en `core-domain` (Kotlin puro, sin javax.inject), así
 * que se proveen aquí.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun providePasswordHasher(): PasswordHasher = Pbkdf2PasswordHasher()

    @Provides
    fun provideLoginUseCase(repository: AuthRepository) = LoginUseCase(repository)

    @Provides
    fun provideRegisterUseCase(repository: AuthRepository) = RegisterUseCase(repository)

    @Provides
    fun provideResetPasswordUseCase(repository: AuthRepository) = ResetPasswordUseCase(repository)

    @Provides
    fun provideLogoutUseCase(repository: AuthRepository) = LogoutUseCase(repository)

    @Provides
    fun provideObserveSessionUseCase(repository: AuthRepository) = ObserveSessionUseCase(repository)
}

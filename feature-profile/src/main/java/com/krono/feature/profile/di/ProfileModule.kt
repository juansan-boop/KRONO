package com.krono.feature.profile.di

import android.content.Context
import com.krono.feature.profile.data.AndroidConnectivityChecker
import com.krono.feature.profile.data.ConnectivityChecker
import com.krono.feature.profile.data.FirestoreProfileDataSource
import com.krono.feature.profile.data.ProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileRepositoryImpl
import com.krono.feature.profile.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias para el módulo `:feature-profile`.
 *
 * Vincula [ProfileRepository] con [ProfileRepositoryImpl] y [ProfileRemoteDataSource]
 * con [FirestoreProfileDataSource], residiendo en `:feature-profile` para mantener el
 * aislamiento estricto de módulos definido en la decisión D-8, y provee el [ConnectivityChecker] de Android.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    @Singleton
    abstract fun bindProfileRemoteDataSource(
        impl: FirestoreProfileDataSource,
    ): ProfileRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl,
    ): ProfileRepository

    companion object {

        /** Provee la comprobación de conectividad basada en [Context] de la aplicación. */
        @Provides
        @Singleton
        fun provideConnectivityChecker(@ApplicationContext context: Context): ConnectivityChecker =
            AndroidConnectivityChecker(context)
    }
}

package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Implementación de reserva en memoria para [AuthRepository] que soporta
 * inicialización sin dependencias externas en constructores secundarios y pruebas.
 */
internal class NoOpAuthRepository(
    private val initialUser: User? = null,
) : AuthRepository {
    override fun observeSession(): Flow<User?> = MutableStateFlow(initialUser)
    override suspend fun login(email: String, password: String): KronoResult<User> =
        KronoResult.Error(UnsupportedOperationException())
    override suspend fun register(email: String, password: String): KronoResult<User> =
        KronoResult.Error(UnsupportedOperationException())
    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> =
        KronoResult.Error(UnsupportedOperationException())
    override suspend fun logout(): KronoResult<Unit> =
        KronoResult.Success(Unit)
}

package com.krono.core.data.auth

import android.database.sqlite.SQLiteConstraintException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.local.AuthDao
import com.krono.core.data.auth.local.SessionEntity
import com.krono.core.data.auth.local.UserEntity
import com.krono.core.data.di.IoDispatcher
import com.krono.core.domain.auth.AuthError
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.User
import com.krono.core.domain.auth.asAuthError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Autenticación local con Room (D-1). Sin red: se reemplazará por un backend
 * implementando el mismo [AuthRepository].
 */
class AuthRepositoryImpl @Inject constructor(
    private val dao: AuthDao,
    private val hasher: PasswordHasher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override fun observeSession(): Flow<User?> = dao.observeSessionUser()
        .map { it?.toDomain() }
        .distinctUntilChanged()
        // Si la base de datos falla al leer la sesión, se trata como "sin sesión" (va al login).
        .catch { emit(null) }

    override suspend fun login(email: String, password: String): KronoResult<User> = runCatchingAuth {
        val user = dao.findByEmail(email)
        if (user == null || !hasher.verify(password, user.toHashedPassword())) {
            return@runCatchingAuth KronoResult.Error(AuthError.InvalidCredentials)
        }
        dao.saveSession(SessionEntity(userId = user.id))
        KronoResult.Success(user.toDomain())
    }

    override suspend fun register(email: String, password: String): KronoResult<User> = runCatchingAuth {
        if (dao.findByEmail(email) != null) {
            return@runCatchingAuth KronoResult.Error(AuthError.EmailAlreadyRegistered)
        }
        val hashed = hasher.hash(password)
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            email = email,
            passwordHash = hashed.hash,
            passwordSalt = hashed.salt,
            passwordIterations = hashed.iterations,
        )
        try {
            dao.insertUser(user)
        } catch (e: SQLiteConstraintException) {
            // Otro registro con el mismo correo se adelantó entre la consulta y la inserción.
            return@runCatchingAuth KronoResult.Error(AuthError.EmailAlreadyRegistered)
        }
        dao.saveSession(SessionEntity(userId = user.id))
        KronoResult.Success(user.toDomain())
    }

    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> = runCatchingAuth {
        // Simulado mientras no haya backend: solo se comprueba que la cuenta exista, no se envía correo.
        if (dao.findByEmail(email) == null) {
            KronoResult.Error(AuthError.EmailNotRegistered)
        } else {
            KronoResult.Success(Unit)
        }
    }

    override suspend fun logout(): KronoResult<Unit> = runCatchingAuth {
        dao.clearSession()
        KronoResult.Success(Unit)
    }

    /** Ejecuta en IO y convierte cualquier excepción en `KronoResult.Error` con un [AuthError]. */
    private suspend fun <T> runCatchingAuth(block: suspend () -> KronoResult<T>): KronoResult<T> =
        withContext(ioDispatcher) {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                KronoResult.Error(e.asAuthError())
            }
        }
}

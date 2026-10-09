package com.krono.core.data.auth

import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.krono.core.common.KronoResult
import com.krono.core.domain.auth.AuthRepository
import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * [AuthRepository] con Firebase Authentication (D-1). Las excepciones del SDK se
 * traducen con [FirebaseAuthErrorMapper] y viajan como `KronoResult.Error`, salvo la
 * cancelación de la corrutina, que se relanza; nunca se registran correos ni contraseñas.
 */
class FirebaseAuthRepository @Inject constructor(
    private val dataSource: AuthRemoteDataSource,
) : AuthRepository {

    /** Emite la sesión actual sin repetir valores iguales consecutivos. */
    override fun observeSession(): Flow<User?> = dataSource.observeUser()
        .distinctUntilChanged()
        // Si no se puede leer la sesión, se trata como "sin sesión" (va al login) en vez de fallar.
        .catch { emit(null) }

    /** Inicia sesión y traduce las excepciones (salvo la cancelación) a un [KronoResult.Error]. */
    override suspend fun login(email: String, password: String): KronoResult<User> =
        runCatchingAuth { dataSource.signIn(email, password) }

    /** Crea la cuenta; el mapeo de errores recibe la contraseña para distinguir una contraseña débil. */
    override suspend fun register(email: String, password: String): KronoResult<User> =
        runCatchingAuth(attemptedPassword = password) { dataSource.createAccount(email, password) }

    /** Solicita el correo de recuperación; responde con éxito aunque el correo no exista. */
    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> = runCatchingAuth {
        try {
            dataSource.sendPasswordReset(email)
        } catch (e: FirebaseAuthInvalidUserException) {
            // Si la protección contra enumeración se desactiva, Firebase avisa que el correo
            // no existe; se responde igual que con éxito para no revelarlo (D-10).
        }
    }

    /** Cierra la sesión. */
    override suspend fun logout(): KronoResult<Unit> = runCatchingAuth { dataSource.signOut() }

    private suspend fun <T> runCatchingAuth(
        attemptedPassword: String = "",
        block: suspend () -> T,
    ): KronoResult<T> = try {
        KronoResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        KronoResult.Error(FirebaseAuthErrorMapper.map(e, attemptedPassword))
    }
}

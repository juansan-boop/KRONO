package com.krono.core.domain.auth

import com.krono.core.common.KronoResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de prueba en memoria de [AuthRepository], compartido vía test fixtures.
 *
 * - [failure]: si no es nulo, toda operación devuelve `KronoResult.Error(failure)`.
 * - [gate]: si no es nulo, las operaciones suspendidas esperan a que se complete
 *   (sirve para observar el estado "cargando" en las pruebas de ViewModel).
 */
class FakeAuthRepository : AuthRepository {

    private val passwordsByEmail = mutableMapOf<String, String>()
    private val session = MutableStateFlow<User?>(null)

    /** Error que devuelven todas las operaciones; `null` para que tengan éxito. */
    var failure: Throwable? = null
    /** Compuerta que retiene las operaciones hasta que se complete. */
    var gate: CompletableDeferred<Unit>? = null

    /** Veces que se llamó a [login]. */
    var loginCalls = 0
        private set
    /** Veces que se llamó a [register]. */
    var registerCalls = 0
        private set
    /** Veces que se llamó a [requestPasswordReset]. */
    var resetCalls = 0
        private set
    /** Último correo recibido por login, registro o recuperación. */
    var lastEmailReceived: String? = null
        private set

    /** Usuario con sesión activa, o `null`. */
    val currentSession: User? get() = session.value

    /** Deja registrada una cuenta sin iniciar sesión. */
    fun givenRegisteredUser(email: String, password: String) {
        passwordsByEmail[email] = password
    }

    /** Deja [user] con la sesión ya iniciada. */
    fun givenActiveSession(user: User) {
        session.value = user
    }

    /** Emite la sesión en memoria. */
    override fun observeSession(): Flow<User?> = session

    /** Éxito si el par correo/contraseña está registrado; si no, [AuthError.InvalidCredentials]. */
    override suspend fun login(email: String, password: String): KronoResult<User> {
        loginCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        if (passwordsByEmail[email] != password) return KronoResult.Error(AuthError.InvalidCredentials)
        return KronoResult.Success(startSession(email))
    }

    /** Registra la cuenta e inicia sesión; [AuthError.EmailAlreadyRegistered] si ya existe. */
    override suspend fun register(email: String, password: String): KronoResult<User> {
        registerCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        if (email in passwordsByEmail) return KronoResult.Error(AuthError.EmailAlreadyRegistered)
        passwordsByEmail[email] = password
        return KronoResult.Success(startSession(email))
    }

    /** Siempre éxito, salvo [failure]; no revela si el correo existe. */
    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> {
        resetCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        // Como Firebase con la protección contra enumeración: éxito aunque el correo no exista (D-10).
        return KronoResult.Success(Unit)
    }

    /** Limpia la sesión en memoria, o devuelve [failure]. */
    override suspend fun logout(): KronoResult<Unit> {
        failure?.let { return KronoResult.Error(it) }
        session.value = null
        return KronoResult.Success(Unit)
    }

    private suspend fun awaitGate() {
        gate?.await()
    }

    private fun startSession(email: String): User = User(id = "id-$email", email = email).also { session.value = it }
}

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

    var failure: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null

    var loginCalls = 0
        private set
    var registerCalls = 0
        private set
    var resetCalls = 0
        private set
    var lastEmailReceived: String? = null
        private set

    val currentSession: User? get() = session.value

    fun givenRegisteredUser(email: String, password: String) {
        passwordsByEmail[email] = password
    }

    fun givenActiveSession(user: User) {
        session.value = user
    }

    override fun observeSession(): Flow<User?> = session

    override suspend fun login(email: String, password: String): KronoResult<User> {
        loginCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        if (passwordsByEmail[email] != password) return KronoResult.Error(AuthError.InvalidCredentials)
        return KronoResult.Success(startSession(email))
    }

    override suspend fun register(email: String, password: String): KronoResult<User> {
        registerCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        if (email in passwordsByEmail) return KronoResult.Error(AuthError.EmailAlreadyRegistered)
        passwordsByEmail[email] = password
        return KronoResult.Success(startSession(email))
    }

    override suspend fun requestPasswordReset(email: String): KronoResult<Unit> {
        resetCalls++
        lastEmailReceived = email
        awaitGate()
        failure?.let { return KronoResult.Error(it) }
        if (email !in passwordsByEmail) return KronoResult.Error(AuthError.EmailNotRegistered)
        return KronoResult.Success(Unit)
    }

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

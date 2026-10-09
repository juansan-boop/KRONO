package com.krono.core.data.auth

import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Doble de [AuthRemoteDataSource] para probar [FirebaseAuthRepository] sin Firebase.
 * Si [failure] no es nulo, cada operación lanza esa excepción, como haría el SDK.
 */
class FakeAuthRemoteDataSource : AuthRemoteDataSource {

    /** Excepción que lanzan todas las operaciones; `null` para que tengan éxito. */
    var failure: Exception? = null

    /** Usuario con sesión, visible para las aserciones. */
    val user = MutableStateFlow<User?>(null)

    /** Correos a los que se pidió enviar el enlace de recuperación. */
    val resetEmailsSent = mutableListOf<String>()

    override fun observeUser(): Flow<User?> = user

    override suspend fun signIn(email: String, password: String): User = startSession(email)

    override suspend fun createAccount(email: String, password: String): User = startSession(email)

    override suspend fun sendPasswordReset(email: String) {
        failure?.let { throw it }
        resetEmailsSent += email
    }

    override fun signOut() {
        failure?.let { throw it }
        user.value = null
    }

    private fun startSession(email: String): User {
        failure?.let { throw it }
        return User(id = "uid-$email", email = email).also { user.value = it }
    }
}

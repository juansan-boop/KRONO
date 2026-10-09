package com.krono.core.data.auth

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.krono.core.domain.auth.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * [AuthRemoteDataSource] sobre Firebase Authentication con correo y contraseña.
 * Firebase persiste la sesión en el dispositivo, así que sobrevive a reiniciar la
 * app (F-22 a F-25 CA-6).
 */
class FirebaseAuthDataSource @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRemoteDataSource {

    /** Emite el usuario de Firebase cada vez que cambia la sesión. */
    override fun observeUser(): Flow<User?> = callbackFlow {
        // Firebase invoca el listener al registrarlo, así que la sesión actual llega de inmediato.
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toDomain()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Inicia sesión con correo y contraseña; las fallas del SDK se propagan como excepción. */
    override suspend fun signIn(email: String, password: String): User =
        auth.signInWithEmailAndPassword(email, password).await().requireUser()

    /** Crea la cuenta en Firebase y deja la sesión iniciada. */
    override suspend fun createAccount(email: String, password: String): User =
        auth.createUserWithEmailAndPassword(email, password).await().requireUser()

    /** Pide a Firebase el correo de recuperación de contraseña. */
    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    /** Cierra la sesión local de Firebase. */
    override fun signOut() {
        auth.signOut()
    }

    private fun AuthResult.requireUser(): User =
        checkNotNull(user) { "Firebase completó la operación sin devolver usuario" }.toDomain()

    private fun FirebaseUser.toDomain(): User = User(id = uid, email = email.orEmpty())
}

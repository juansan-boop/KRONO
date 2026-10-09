package com.krono.core.data.auth

import com.krono.core.domain.auth.User
import kotlinx.coroutines.flow.Flow

/**
 * Operaciones crudas del proveedor de autenticación. A diferencia de
 * `AuthRepository`, lanza las excepciones del SDK: [FirebaseAuthRepository] las
 * traduce. Existe para poder probar el repositorio sin Firebase.
 */
interface AuthRemoteDataSource {

    /** Usuario con sesión cada vez que cambia, o `null` sin sesión. */
    fun observeUser(): Flow<User?>

    /** Inicia sesión con correo y contraseña. */
    suspend fun signIn(email: String, password: String): User

    /** Crea la cuenta; el proveedor deja la sesión iniciada. */
    suspend fun createAccount(email: String, password: String): User

    /** Envía el correo con el enlace para restablecer la contraseña. */
    suspend fun sendPasswordReset(email: String)

    /** Cierra la sesión local del proveedor. */
    fun signOut()
}

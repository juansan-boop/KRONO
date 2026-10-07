package com.krono.core.data.auth

import com.krono.core.data.auth.local.AuthDao
import com.krono.core.data.auth.local.SessionEntity
import com.krono.core.data.auth.local.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** DAO en memoria que imita las restricciones de Room (correo único, una sola sesión). */
class FakeAuthDao : AuthDao {

    private val users = MutableStateFlow<Map<String, UserEntity>>(emptyMap())
    private val session = MutableStateFlow<SessionEntity?>(null)

    var failure: Exception? = null

    val storedUsers: Collection<UserEntity> get() = users.value.values
    val storedSession: SessionEntity? get() = session.value

    override suspend fun findByEmail(email: String): UserEntity? {
        failure?.let { throw it }
        return users.value.values.firstOrNull { it.email == email }
    }

    override suspend fun insertUser(user: UserEntity) {
        failure?.let { throw it }
        check(users.value.values.none { it.email == user.email }) { "UNIQUE constraint failed: users.email" }
        users.value = users.value + (user.id to user)
    }

    override suspend fun saveSession(session: SessionEntity) {
        failure?.let { throw it }
        this.session.value = session
    }

    override suspend fun clearSession() {
        failure?.let { throw it }
        session.value = null
    }

    override fun observeSessionUser(): Flow<UserEntity?> =
        combine(users, session) { usuarios, sesion -> sesion?.let { usuarios[it.userId] } }
}

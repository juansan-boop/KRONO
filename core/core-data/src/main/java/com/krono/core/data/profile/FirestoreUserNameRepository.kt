package com.krono.core.data.profile

import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.domain.profile.UserNameRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Implementación de [UserNameRepository] con Cloud Firestore: lee el nombre del perfil del usuario. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreUserNameRepository @Inject constructor(
    private val remoteDataSource: UserNameRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : UserNameRepository {

    /** Emite el nombre del perfil, o `null` sin sesión o si la lectura falla (el saludo es opcional). */
    override fun observeUserName(): Flow<String?> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) flowOf(null) else remoteDataSource.observeUserName(user.id)
            }
            .catch { emit(null) }
}

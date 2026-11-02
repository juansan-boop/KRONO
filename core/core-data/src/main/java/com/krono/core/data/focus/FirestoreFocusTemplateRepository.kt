package com.krono.core.data.focus

import com.google.firebase.firestore.FirebaseFirestoreException
import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.firestore.awaitConfirmationOrQueued
import com.krono.core.domain.auth.User
import com.krono.core.domain.focus.FocusTemplate
import com.krono.core.domain.focus.FocusTemplateChanges
import com.krono.core.domain.focus.FocusTemplateError
import com.krono.core.domain.focus.FocusTemplateRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [FocusTemplateRepository] con Cloud Firestore (F-10, D-1): las plantillas
 * viven bajo `users/{uid}/focusTemplates`.
 *
 * Cada escritura espera la confirmación del servidor con [awaitConfirmationOrQueued]: sin red se
 * da por encolada y se informa como éxito, y los errores que llegan antes del límite (permiso
 * denegado, datos inválidos) se devuelven como [KronoResult.Error].
 *
 * Limitación de la siembra: si el dispositivo no ha leído nunca el servidor (primer inicio sin red)
 * y la caché está vacía, se siembran las plantillas por defecto aunque la cuenta ya las tuviera
 * editadas en otro dispositivo; al reconectar, los ids fijos de los valores por defecto
 * sobrescribirían esas ediciones.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreFocusTemplateRepository @Inject constructor(
    private val remoteDataSource: FocusTemplateRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : FocusTemplateRepository {

    /** Emite las plantillas del usuario con sesión; si el listener falla emite un error y termina. */
    override fun observeTemplates(): Flow<KronoResult<List<FocusTemplate>>> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    remoteDataSource.observeTemplates(user.id).map { dtos -> dtos.map { it.toDomain() } }
                }
            }
            .map<List<FocusTemplate>, KronoResult<List<FocusTemplate>>> { KronoResult.Success(it) }
            .catch { emit(KronoResult.Error(mapError(it))) }

    override suspend fun getTemplates(): KronoResult<List<FocusTemplate>> = runCatchingTemplates {
        remoteDataSource.getTemplates(requireUser().id).map { it.toDomain() }
    }

    override suspend fun seedDefaultsIfEmpty(defaults: List<FocusTemplate>): KronoResult<Unit> =
        runCatchingTemplates {
            val userId = requireUser().id
            if (remoteDataSource.getTemplates(userId).isEmpty()) {
                awaitConfirmationOrQueued {
                    remoteDataSource.commit(userId, defaults.map(FocusTemplateDto::fromDomain), emptyList())
                }
            }
        }

    override suspend fun applyChanges(changes: FocusTemplateChanges): KronoResult<Unit> = runCatchingTemplates {
        val userId = requireUser().id
        awaitConfirmationOrQueued {
            remoteDataSource.commit(userId, changes.upserts.map(FocusTemplateDto::fromDomain), changes.deletedIds)
        }
    }

    private fun requireUser(): User = authDataSource.currentUser ?: throw FocusTemplateError.UnauthenticatedUser

    private suspend fun <T> runCatchingTemplates(block: suspend () -> T): KronoResult<T> = try {
        KronoResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        KronoResult.Error(mapError(t))
    }

    /** Traduce una excepción técnica a un error de dominio; el de dominio pasa tal cual. */
    private fun mapError(throwable: Throwable): FocusTemplateError = when (throwable) {
        is FocusTemplateError -> throwable
        is FirebaseFirestoreException -> when (throwable.code) {
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> FocusTemplateError.UnauthenticatedUser
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                FocusTemplateError.StorageError("Permiso denegado para acceder a las plantillas.", throwable)
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                FocusTemplateError.StorageError("Servicio de base de datos no disponible temporalmente.", throwable)
            else -> FocusTemplateError.StorageError(throwable.message ?: "Error de Firestore", throwable)
        }
        else -> FocusTemplateError.StorageError(throwable.message ?: "Error desconocido en persistencia", throwable)
    }
}

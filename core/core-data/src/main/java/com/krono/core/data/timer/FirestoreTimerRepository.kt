package com.krono.core.data.timer

import com.krono.core.common.KronoResult
import com.krono.core.data.auth.AuthRemoteDataSource
import com.krono.core.data.firestore.WRITE_CONFIRMATION_TIMEOUT
import com.krono.core.data.firestore.awaitConfirmationOrQueued
import com.krono.core.data.firestore.isUnavailable
import com.krono.core.data.task.FirestoreTaskErrorMapper
import com.krono.core.domain.task.TaskError
import com.krono.core.domain.timer.ActiveTimer
import com.krono.core.domain.timer.FinishedSession
import com.krono.core.domain.timer.TimeSession
import com.krono.core.domain.timer.TimerError
import com.krono.core.domain.timer.TimerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Implementación de [TimerRepository] con Cloud Firestore (F-08, D-1): el cronómetro en
 * curso y las sesiones viven bajo `users/{uid}`, así sobreviven a cerrar la app (CA-3).
 *
 * Igual que las tareas, cada escritura espera la confirmación del servidor con
 * [awaitConfirmationOrQueued]: sin red se da por encolada y se informa como éxito, y los errores
 * que llegan antes del límite (permiso denegado, datos inválidos) se devuelven como
 * [KronoResult.Error] en vez de perderse. Las lecturas de comprobación tampoco bloquean: sin red
 * usan la caché local (ver [TimerRemoteDataSource.getActiveTimer]).
 *
 * Debe ser una única instancia (está enlazado como `@Singleton`): la exclusión mutua de
 * [finishActiveTimer] y el recuerdo de la última sesión finalizada son de la instancia.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreTimerRepository @Inject constructor(
    private val remoteDataSource: TimerRemoteDataSource,
    private val authDataSource: AuthRemoteDataSource,
) : TimerRepository {

    /** Serializa las finalizaciones para que dos llamadas simultáneas no sumen dos veces. */
    private val finishMutex = Mutex()

    /** Última sesión finalizada, para responder igual a una finalización repetida. */
    private var lastFinished: FinishedSession? = null

    /** Emite el cronómetro en curso del usuario con sesión activa. */
    override fun observeActiveTimer(): Flow<ActiveTimer?> =
        authDataSource.observeUser()
            .distinctUntilChanged()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(null)
                } else {
                    remoteDataSource.observeActiveTimer(user.id).map { it?.let(ActiveTimerDto::fromMap) }
                }
            }
            .catch { emit(null) }

    /**
     * Lee una vez el cronómetro en curso. Sin red y sin el documento en la caché local no se sabe
     * si hay uno, y se responde que no: así iniciar sin conexión funciona. La pantalla del
     * cronómetro observa ese mismo documento y en ese caso también muestra que no hay ninguno.
     */
    override suspend fun getActiveTimer(): KronoResult<ActiveTimer?> = runCatchingTimer { userId ->
        try {
            readActiveTimer(userId)
        } catch (offline: Exception) {
            if (!offline.isUnavailable()) throw offline
            null
        }
    }

    /** Guarda el cronómetro en curso. */
    override suspend fun saveActiveTimer(timer: ActiveTimer): KronoResult<Unit> = runCatchingTimer { userId ->
        awaitConfirmationOrQueued { remoteDataSource.saveActiveTimer(userId, ActiveTimerDto.toMap(timer)) }
    }

    /**
     * Registra la sesión, elimina el cronómetro en curso y suma la duración a la tarea.
     *
     * Las finalizaciones se serializan y cada una vuelve a leer el cronómetro en curso antes de
     * escribir: si ya no es el de [session] (otra llamada lo acaba de finalizar), no se escribe
     * nada. Si esa otra llamada finalizó esta misma sesión, se devuelve su resultado, así un
     * doble toque no suma dos veces ni muestra un error espurio.
     *
     * La sesión y el incremento son escrituras separadas (ver
     * [TimerRemoteDataSource.finishActiveTimer]): si la tarea se borró, aunque sea en otro
     * dispositivo y sin red, solo se rechaza el incremento, y la sesión y el borrado del
     * cronómetro se conservan. A cambio se pierde la atomicidad del lote: si el servidor
     * rechazara solo la sesión (las reglas de ambas rutas son las mismas, así que no se espera),
     * el tiempo quedaría sumado sin sesión. Como `actualDuration` es la suma de las sesiones de la
     * tarea (Sección 6 del SPEC), cualquier desajuste se reconcilia recalculándolo desde
     * `timeSessions`.
     */
    override suspend fun finishActiveTimer(session: TimeSession): KronoResult<FinishedSession> =
        runCatchingTimer { userId ->
            finishMutex.withLock {
                if (isStillActive(userId, session)) {
                    commitFinish(userId, session).also { lastFinished = it }
                } else {
                    lastFinished?.takeIf { it.session.id == session.id } ?: throw TimerError.NoActiveTimer
                }
            }
        }

    /**
     * Indica si el cronómetro en curso es el que produce [session]. Si no se puede leer (sin red y
     * sin caché) se asume que sí: quien llama acaba de leerlo para construir la sesión.
     */
    private suspend fun isStillActive(userId: String, session: TimeSession): Boolean = try {
        readActiveTimer(userId)?.sessionId == session.id
    } catch (offline: Exception) {
        if (!offline.isUnavailable()) throw offline
        true
    }

    /**
     * Escribe la sesión y el incremento. Si la confirmación no llega a tiempo (sin red), ambas
     * quedaron encoladas y se informa que el tiempo se sumó; si al reconectar la tarea ya no
     * existe, solo se descarta el incremento.
     */
    private suspend fun commitFinish(userId: String, session: TimeSession): FinishedSession {
        val addedToTask = withTimeoutOrNull(WRITE_CONFIRMATION_TIMEOUT) {
            remoteDataSource.finishActiveTimer(
                userId = userId,
                session = TimeSessionDto.toMap(session),
                taskId = session.taskId,
                durationMillis = session.duration.inWholeMilliseconds,
            )
        } ?: true
        return FinishedSession(session, addedToTask)
    }

    private suspend fun readActiveTimer(userId: String): ActiveTimer? =
        remoteDataSource.getActiveTimer(userId)?.let(ActiveTimerDto::fromMap)

    private suspend fun <T> runCatchingTimer(block: suspend (userId: String) -> T): KronoResult<T> = try {
        val user = authDataSource.currentUser ?: throw TaskError.UnauthenticatedUser
        KronoResult.Success(block(user.id))
    } catch (e: CancellationException) {
        throw e
    } catch (timerError: TimerError) {
        KronoResult.Error(timerError)
    } catch (t: Throwable) {
        KronoResult.Error(FirestoreTaskErrorMapper.map(t))
    }
}

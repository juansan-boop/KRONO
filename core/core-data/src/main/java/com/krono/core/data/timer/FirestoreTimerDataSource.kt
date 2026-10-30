package com.krono.core.data.timer

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.krono.core.data.firestore.readFromServerOrCache
import com.krono.core.data.task.TaskDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [TimerRemoteDataSource] con Cloud Firestore (F-08).
 *
 * Las escrituras suspenden hasta la confirmación del servidor, que sin red no llega: quien las
 * llama debe acotar esa espera (ver [FirestoreTimerRepository]). Así los errores reales (permiso
 * denegado, datos inválidos, tarea inexistente) llegan a quien escribe en vez de perderse.
 */
class FirestoreTimerDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TimerRemoteDataSource {

    private fun userDocument(userId: String) = firestore.collection(COLLECTION_USERS).document(userId)

    private fun activeTimerDocument(userId: String) = TimerDocuments.activeTimer(firestore, userId)

    override fun observeActiveTimer(userId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val listener = activeTimerDocument(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.data)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getActiveTimer(userId: String): Map<String, Any?>? {
        val document = activeTimerDocument(userId)
        return readFromServerOrCache(
            fromServer = { document.get().await() },
            fromCache = { document.get(Source.CACHE).await() },
        ).data
    }

    override suspend fun saveActiveTimer(userId: String, timer: Map<String, Any?>) {
        activeTimerDocument(userId).set(timer).await()
    }

    override suspend fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
    ): Boolean {
        val sessionId = session[TimeSessionDto.FIELD_ID] as String
        // Ambas escrituras se encolan antes de esperar a ninguna: así no queda una ventana en la
        // que el proceso muera con la sesión guardada y el incremento sin encolar.
        val sessionBatch = firestore.batch()
            .set(userDocument(userId).collection(COLLECTION_TIME_SESSIONS).document(sessionId), session)
            .delete(activeTimerDocument(userId))
            .commit()
        // increment suma en el servidor: no pisa tiempo registrado desde otro dispositivo.
        val increment = userDocument(userId).collection(COLLECTION_TASKS).document(taskId)
            .update(TaskDto.FIELD_ACTUAL_DURATION_MILLIS, FieldValue.increment(durationMillis))
        sessionBatch.await()
        return awaitIncrement(increment)
    }

    /** Espera el incremento; `false` si la tarea ya no existe (`NOT_FOUND`). */
    private suspend fun awaitIncrement(increment: Task<Void>): Boolean = try {
        increment.await()
        true
    } catch (notFound: FirebaseFirestoreException) {
        if (notFound.code != FirebaseFirestoreException.Code.NOT_FOUND) throw notFound
        false
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_TASKS = "tasks"
        const val COLLECTION_TIME_SESSIONS = "timeSessions"
    }
}

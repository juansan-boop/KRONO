package com.krono.core.data.timer

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.krono.core.data.task.TaskDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [TimerRemoteDataSource] con Cloud Firestore (F-08).
 *
 * Las escrituras no esperan la confirmación del servidor: Firestore las aplica al instante
 * en su caché local (y los listeners las ven), y las sincroniza cuando hay conexión. Esperar
 * con `await()` dejaría los botones del cronómetro bloqueados sin internet.
 */
class FirestoreTimerDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TimerRemoteDataSource {

    private fun userDocument(userId: String) = firestore.collection(COLLECTION_USERS).document(userId)

    private fun activeTimerDocument(userId: String) =
        userDocument(userId).collection(COLLECTION_TIMER).document(DOCUMENT_ACTIVE)

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

    override suspend fun getActiveTimer(userId: String): Map<String, Any?>? =
        activeTimerDocument(userId).get().await().data

    override fun saveActiveTimer(userId: String, timer: Map<String, Any?>) {
        activeTimerDocument(userId).set(timer)
    }

    override suspend fun taskExists(userId: String, taskId: String): Boolean =
        userDocument(userId).collection(COLLECTION_TASKS).document(taskId).get().await().exists()

    override fun finishActiveTimer(
        userId: String,
        session: Map<String, Any?>,
        taskId: String,
        durationMillis: Long,
        addToTask: Boolean,
    ) {
        val sessionId = session[TimeSessionDto.FIELD_ID] as String
        val batch = firestore.batch()
        batch.set(userDocument(userId).collection(COLLECTION_TIME_SESSIONS).document(sessionId), session)
        if (addToTask) {
            // increment suma en el servidor: no pisa tiempo registrado desde otro dispositivo.
            batch.update(
                userDocument(userId).collection(COLLECTION_TASKS).document(taskId),
                TaskDto.FIELD_ACTUAL_DURATION_MILLIS,
                FieldValue.increment(durationMillis),
            )
        }
        batch.delete(activeTimerDocument(userId))
        batch.commit()
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_TASKS = "tasks"
        const val COLLECTION_TIMER = "timer"
        const val COLLECTION_TIME_SESSIONS = "timeSessions"
        const val DOCUMENT_ACTIVE = "active"
    }
}

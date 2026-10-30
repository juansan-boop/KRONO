package com.krono.core.data.task

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.krono.core.data.firestore.readFromServerOrCache
import com.krono.core.data.timer.TimerDocuments
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [TaskRemoteDataSource] conectada a Cloud Firestore (D-1, D-3, F-02).
 * Opera bajo la ruta `users/{userId}/tasks/{taskId}` con persistencia offline de Firestore.
 *
 * Las escrituras suspenden hasta la confirmación del servidor, que sin red no llega: quien las
 * llama debe acotar esa espera (ver [FirestoreTaskRepository]).
 */
class FirestoreTaskDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TaskRemoteDataSource {

    private fun tasksCollection(userId: String) =
        firestore.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_TASKS)

    override fun observeTasks(userId: String): Flow<List<TaskDto>> = callbackFlow {
        val listener: ListenerRegistration = tasksCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { TaskDto.fromMap(doc.id, it) }
                }.orEmpty()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    override fun observeTask(userId: String, taskId: String): Flow<TaskDto?> = callbackFlow {
        val listener: ListenerRegistration = tasksCollection(userId).document(taskId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val task = snapshot?.data?.let { TaskDto.fromMap(snapshot.id, it) }
                trySend(task)
            }
        awaitClose { listener.remove() }
    }

    // Con límite de espera y caché local: la comprobación de un cronómetro huérfano al iniciar
    // no debe bloquear al usuario sin red.
    override suspend fun getTask(userId: String, taskId: String): TaskDto? {
        val document = tasksCollection(userId).document(taskId)
        val snapshot = readFromServerOrCache(
            fromServer = { document.get().await() },
            fromCache = { document.get(Source.CACHE).await() },
        )
        return snapshot.data?.let { TaskDto.fromMap(snapshot.id, it) }
    }

    override suspend fun saveTask(userId: String, taskDto: TaskDto) {
        tasksCollection(userId).document(taskDto.id).set(taskDto.toMap()).await()
    }

    // update (y no set) para que los campos que no vienen en el mapa, como el tiempo real que
    // suma el cronómetro con increment, conserven su valor del servidor.
    override suspend fun updateTaskFields(userId: String, taskId: String, fields: Map<String, Any?>) {
        tasksCollection(userId).document(taskId).update(fields).await()
    }

    // Un solo lote: o se borran la tarea y su cronómetro en curso, o ninguno de los dos.
    override suspend fun deleteTask(userId: String, taskId: String, discardActiveTimer: Boolean) {
        val batch = firestore.batch()
        batch.delete(tasksCollection(userId).document(taskId))
        if (discardActiveTimer) batch.delete(TimerDocuments.activeTimer(firestore, userId))
        batch.commit().await()
    }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TASKS = "tasks"
    }
}

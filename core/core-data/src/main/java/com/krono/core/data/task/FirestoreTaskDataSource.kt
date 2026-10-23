package com.krono.core.data.task

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementación de [TaskRemoteDataSource] conectada a Cloud Firestore (D-1, D-3, F-02).
 * Opera bajo la ruta `users/{userId}/tasks/{taskId}` con persistencia offline de Firestore.
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

    override suspend fun getTask(userId: String, taskId: String): TaskDto? {
        val snapshot = tasksCollection(userId).document(taskId).get().await()
        return snapshot.data?.let { TaskDto.fromMap(snapshot.id, it) }
    }

    override suspend fun saveTask(userId: String, taskDto: TaskDto) {
        tasksCollection(userId).document(taskDto.id).set(taskDto.toMap()).await()
    }

    override suspend fun deleteTask(userId: String, taskId: String) {
        tasksCollection(userId).document(taskId).delete().await()
    }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_TASKS = "tasks"
    }
}

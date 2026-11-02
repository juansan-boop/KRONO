package com.krono.core.data.timer

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Implementación de [TimeSessionRemoteDataSource] con Cloud Firestore. El filtro es un rango
 * sobre un solo campo, así que no exige un índice compuesto.
 */
class FirestoreTimeSessionDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : TimeSessionRemoteDataSource {

    override fun observeSessionsBetween(
        userId: String,
        fromMillis: Long,
        toMillis: Long,
    ): Flow<List<Map<String, Any?>>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_USERS).document(userId)
            .collection(COLLECTION_TIME_SESSIONS)
            .whereGreaterThanOrEqualTo(TimeSessionDto.FIELD_STARTED_AT_MILLIS, fromMillis)
            .whereLessThan(TimeSessionDto.FIELD_STARTED_AT_MILLIS, toMillis)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.data }.orEmpty())
            }
        awaitClose { listener.remove() }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_TIME_SESSIONS = "timeSessions"
    }
}

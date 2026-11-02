package com.krono.core.data.profile

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Implementación de [UserNameRemoteDataSource] con Cloud Firestore. Solo lee el campo `name`
 * del documento `users/{userId}`; la escritura del perfil es de `feature-profile`.
 */
class FirestoreUserNameDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserNameRemoteDataSource {

    override fun observeUserName(userId: String): Flow<String?> = callbackFlow {
        val listener = firestore.collection(COLLECTION_USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.getString(FIELD_NAME))
            }
        awaitClose { listener.remove() }
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val FIELD_NAME = "name"
    }
}

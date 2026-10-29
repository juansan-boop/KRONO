package com.krono.feature.profile.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ProfileRemoteDataSource] respaldada por Cloud Firestore y Firebase Auth (D-1, D-3, F-21).
 * Opera bajo el documento raíz `users/{userId}` según la Sección 6 de `SPEC.md`.
 *
 * Reside en `:feature-profile/data` para respetar el aislamiento estricto de módulos (decisión D-8),
 * evitando modificaciones invasivas en `:core:core-data`.
 *
 * Maneja de forma tolerante a fallos la ausencia de Firebase durante pruebas JVM locales.
 */
@Singleton
class FirestoreProfileDataSource(
    private val firestoreProvider: () -> FirebaseFirestore? = {
        try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
    },
    private val authProvider: () -> FirebaseAuth? = {
        try { FirebaseAuth.getInstance() } catch (_: Throwable) { null }
    },
    private val serverTimestampProvider: () -> Any = {
        FieldValue.serverTimestamp()
    },
) : ProfileRemoteDataSource {

    @Inject
    constructor() : this(
        firestoreProvider = {
            try { FirebaseFirestore.getInstance() } catch (_: Throwable) { null }
        },
        authProvider = {
            try { FirebaseAuth.getInstance() } catch (_: Throwable) { null }
        },
        serverTimestampProvider = {
            FieldValue.serverTimestamp()
        },
    )

    /**
     * Construye la estructura de datos inicial con todos los valores por defecto del usuario (F-21, D-1).
     */
    internal fun createBootstrapPayload(
        email: String,
        displayName: String? = null,
        existingName: String? = null,
        existingLastName: String? = null,
        existingRole: String? = null,
    ): Map<String, Any?> {
        val baseName = displayName?.trim().takeIf { !it.isNullOrBlank() }
            ?: ProfileLocalStore.formatNameFromEmail(email).ifBlank { DEFAULT_NAME }
        val firstName = baseName.substringBefore(' ')
        val lastName = baseName.substringAfter(' ', "").trim()

        return mapOf(
            FIELD_NAME to (existingName?.takeIf { it.isNotBlank() } ?: firstName),
            FIELD_LAST_NAME to (existingLastName?.takeIf { it.isNotBlank() } ?: lastName),
            FIELD_EMAIL to email,
            FIELD_ROLE to (existingRole?.takeIf { it.isNotBlank() } ?: DEFAULT_ROLE),
            FIELD_AVATAR_URI to null,
            FIELD_DEADLINE_MINUTES to NotificationPrefs.DEFAULT_REMINDER_MINUTES,
            FIELD_FOCUS_SOUND to true,
            FIELD_SOUND_TONE to NotificationPrefs.DEFAULT_SOUND_TONE,
            FIELD_DEADLINE_ENABLED to true,
            FIELD_DYNAMIC_BREAKS to true,
            FIELD_SILENCE_NOTIFS to true,
            FIELD_PRIORITY_ALERTS to true,
            FIELD_UPDATED_AT to serverTimestampProvider(),
        )
    }

    override suspend fun saveUserProfile(
        userId: String,
        name: String,
        role: String,
        avatarUri: String?,
    ): KronoResult<Unit> = runCatchingSafe {
        val trimmedName = name.trim()
        val trimmedRole = role.trim()
        val firstName = trimmedName.substringBefore(' ')
        val lastName = trimmedName.substringAfter(' ', "").trim()
        val currentUserEmail = authProvider()?.currentUser?.email.orEmpty()

        val targetUid = userId.ifBlank { authProvider()?.currentUser?.uid.orEmpty() }
        if (targetUid.isNotBlank()) {
            val firestore = firestoreProvider()
            if (firestore != null) {
                val data = mutableMapOf<String, Any?>(
                    FIELD_NAME to firstName,
                    FIELD_LAST_NAME to lastName,
                    FIELD_EMAIL to currentUserEmail,
                    FIELD_ROLE to trimmedRole,
                    FIELD_AVATAR_URI to avatarUri,
                    FIELD_UPDATED_AT to serverTimestampProvider(),
                )
                firestore.collection(COLLECTION_USERS)
                    .document(targetUid)
                    .set(data, SetOptions.merge())
                    .await()
            }
        }

        // Actualizar displayName en Firebase Authentication
        val auth = authProvider()
        val user = auth?.currentUser
        if (user != null && trimmedName.isNotBlank()) {
            val updateRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(trimmedName)
                .build()
            user.updateProfile(updateRequest).await()
        }
    }

    override suspend fun saveNotificationPrefs(
        userId: String,
        prefs: NotificationPrefs,
    ): KronoResult<Unit> = runCatchingSafe {
        val targetUid = userId.ifBlank { authProvider()?.currentUser?.uid.orEmpty() }
        if (targetUid.isNotBlank()) {
            val firestore = firestoreProvider()
            if (firestore != null) {
                val data = mapOf<String, Any?>(
                    FIELD_DEADLINE_MINUTES to prefs.deadlineReminderMinutes,
                    FIELD_FOCUS_SOUND to prefs.focusStartSound,
                    FIELD_SOUND_TONE to prefs.soundTone,
                    FIELD_DEADLINE_ENABLED to prefs.deadlineRemindersEnabled,
                    FIELD_UPDATED_AT to serverTimestampProvider(),
                )
                firestore.collection(COLLECTION_USERS)
                    .document(targetUid)
                    .set(data, SetOptions.merge())
                    .await()
            }
        }
    }

    override suspend fun saveSettingsToggles(
        userId: String,
        dynamicBreaks: Boolean,
        silenceNotifications: Boolean,
        priorityAlerts: Boolean,
    ): KronoResult<Unit> = runCatchingSafe {
        val targetUid = userId.ifBlank { authProvider()?.currentUser?.uid.orEmpty() }
        if (targetUid.isNotBlank()) {
            val firestore = firestoreProvider()
            if (firestore != null) {
                val data = mapOf<String, Any?>(
                    FIELD_DYNAMIC_BREAKS to dynamicBreaks,
                    FIELD_SILENCE_NOTIFS to silenceNotifications,
                    FIELD_PRIORITY_ALERTS to priorityAlerts,
                    FIELD_UPDATED_AT to serverTimestampProvider(),
                )
                firestore.collection(COLLECTION_USERS)
                    .document(targetUid)
                    .set(data, SetOptions.merge())
                    .await()
            }
        }
    }

    override suspend fun resetUserData(userId: String): KronoResult<Unit> = runProtectedOperation {
        val targetUid = userId.ifBlank { authProvider()?.currentUser?.uid.orEmpty() }
        val firestore = firestoreProvider()
        if (targetUid.isNotBlank() && firestore != null) {
            val defaults = buildDefaultProfile(authProvider()?.currentUser?.email.orEmpty())
            purgeUserData(firestore, targetUid) { batch, userRef ->
                batch.set(userRef, defaults, SetOptions.merge())
            }
        }
    }

    override suspend fun reauthenticate(password: String): KronoResult<Unit> = runProtectedOperation {
        val user = requireCurrentUser()
        val email = user.email ?: throw IllegalStateException("El usuario no tiene correo asociado")
        val credential = EmailAuthProvider.getCredential(email, password)
        withRemoteTimeout {
            user.reauthenticate(credential).await()
            Unit
        }
    }

    override suspend fun deleteAccount(userId: String): KronoResult<Unit> = runProtectedOperation {
        val user = requireCurrentUser()
        val targetUid = userId.ifBlank { user.uid }
        // Los datos se borran primero: si falla, la cuenta de acceso sigue viva y el usuario puede reintentar.
        firestoreProvider()?.let { firestore ->
            purgeUserData(firestore, targetUid) { batch, userRef -> batch.delete(userRef) }
        }
        withRemoteTimeout {
            user.delete().await()
            Unit
        }
    }

    /**
     * Borra las tareas y el cronómetro activo del usuario y aplica [finalOperation] sobre su documento
     * de perfil. El lote final (cronómetro y perfil, más las últimas tareas) es un único [WriteBatch],
     * de modo que esas dos escrituras se encolan juntas o no se encolan; ver [purgeInBatches] para el
     * orden de los lotes y el límite de tiempo por paso.
     *
     * Los identificadores de tareas se leen del servidor ([Source.SERVER]), de modo que sin red falla
     * de inmediato antes de emitir ninguna escritura y no depende de una caché incompleta.
     */
    private suspend fun purgeUserData(
        firestore: FirebaseFirestore,
        userId: String,
        finalOperation: (batch: WriteBatch, userRef: DocumentReference) -> Unit,
    ) {
        val userRef = firestore.collection(COLLECTION_USERS).document(userId)
        purgeInBatches(
            readTaskRefs = {
                userRef.collection(COLLECTION_TASKS).get(Source.SERVER).await().documents.map { it.reference }
            },
            commitTaskBatch = { taskRefs ->
                val batch = firestore.batch()
                taskRefs.forEach(batch::delete)
                batch.commit().await()
            },
            commitFinalBatch = { taskRefs ->
                val batch = firestore.batch()
                taskRefs.forEach(batch::delete)
                batch.delete(userRef.collection(COLLECTION_TIMER).document(DOCUMENT_ACTIVE_TIMER))
                finalOperation(batch, userRef)
                batch.commit().await()
            },
        )
    }

    /** Documento de perfil con todos los valores por defecto, conservando el correo de la sesión. */
    private fun buildDefaultProfile(email: String): Map<String, Any?> = mapOf(
        FIELD_NAME to DEFAULT_NAME,
        FIELD_LAST_NAME to "",
        FIELD_EMAIL to email,
        FIELD_ROLE to DEFAULT_ROLE,
        FIELD_AVATAR_URI to null,
        FIELD_DEADLINE_MINUTES to NotificationPrefs.DEFAULT_REMINDER_MINUTES,
        FIELD_FOCUS_SOUND to true,
        FIELD_SOUND_TONE to NotificationPrefs.DEFAULT_SOUND_TONE,
        FIELD_DEADLINE_ENABLED to true,
        FIELD_DYNAMIC_BREAKS to true,
        FIELD_SILENCE_NOTIFS to true,
        FIELD_PRIORITY_ALERTS to true,
        FIELD_UPDATED_AT to serverTimestampProvider(),
    )

    /**
     * Ejecuta una operación de F-21 devolviendo [KronoResult]: traduce los errores de Firebase a
     * errores del dominio y deja pasar la cancelación para que "Cancelar" detenga la espera.
     */
    private suspend fun runProtectedOperation(block: suspend () -> Unit): KronoResult<Unit> = try {
        block()
        KronoResult.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        KronoResult.Error(error.toProfileOperationError())
    }

    private fun requireCurrentUser(): FirebaseUser =
        authProvider()?.currentUser ?: throw IllegalStateException("No hay sesión activa")

    override suspend fun syncFromRemote(
        userId: String,
        localStore: ProfileLocalStore,
    ): KronoResult<Unit> = runCatchingSafe {
        val targetUid = userId.ifBlank { authProvider()?.currentUser?.uid.orEmpty() }
        if (targetUid.isBlank()) return@runCatchingSafe
        val firestore = firestoreProvider() ?: return@runCatchingSafe

        val snapshot = firestore.collection(COLLECTION_USERS)
            .document(targetUid)
            .get()
            .await()

        val auth = authProvider()
        val authUser = auth?.currentUser
        val currentUserEmail = authUser?.email.orEmpty()
        val authDisplayName = authUser?.displayName.orEmpty().trim()
        val baseName = authDisplayName.ifBlank {
            ProfileLocalStore.formatNameFromEmail(currentUserEmail).ifBlank { DEFAULT_NAME }
        }
        val firstName = baseName.substringBefore(' ')
        val lastName = baseName.substringAfter(' ', "").trim()

        val exists = snapshot.exists()
        val remoteName = if (exists) snapshot.getString(FIELD_NAME) else null
        val remoteEmail = if (exists) snapshot.getString(FIELD_EMAIL) else null
        val remoteRole = if (exists) snapshot.getString(FIELD_ROLE) else null

        val hasBaseFields = exists &&
            !remoteName.isNullOrBlank() &&
            !remoteEmail.isNullOrBlank() &&
            !remoteRole.isNullOrBlank()

        if (!exists || !hasBaseFields) {
            val initialData = mutableMapOf<String, Any?>(
                FIELD_NAME to (remoteName?.takeIf { it.isNotBlank() } ?: firstName),
                FIELD_LAST_NAME to (if (exists) snapshot.getString(FIELD_LAST_NAME).orEmpty() else lastName),
                FIELD_EMAIL to (remoteEmail?.takeIf { it.isNotBlank() } ?: currentUserEmail),
                FIELD_ROLE to (remoteRole?.takeIf { it.isNotBlank() } ?: DEFAULT_ROLE),
                FIELD_AVATAR_URI to (if (exists) snapshot.getString(FIELD_AVATAR_URI) else null),
                FIELD_DEADLINE_MINUTES to (if (exists && snapshot.contains(FIELD_DEADLINE_MINUTES)) snapshot.getLong(FIELD_DEADLINE_MINUTES)?.toInt() else NotificationPrefs.DEFAULT_REMINDER_MINUTES),
                FIELD_FOCUS_SOUND to (if (exists && snapshot.contains(FIELD_FOCUS_SOUND)) snapshot.getBoolean(FIELD_FOCUS_SOUND) else true),
                FIELD_SOUND_TONE to (if (exists && snapshot.contains(FIELD_SOUND_TONE)) snapshot.getString(FIELD_SOUND_TONE) else NotificationPrefs.DEFAULT_SOUND_TONE),
                FIELD_DEADLINE_ENABLED to (if (exists && snapshot.contains(FIELD_DEADLINE_ENABLED)) snapshot.getBoolean(FIELD_DEADLINE_ENABLED) else true),
                FIELD_DYNAMIC_BREAKS to (if (exists && snapshot.contains(FIELD_DYNAMIC_BREAKS)) snapshot.getBoolean(FIELD_DYNAMIC_BREAKS) else true),
                FIELD_SILENCE_NOTIFS to (if (exists && snapshot.contains(FIELD_SILENCE_NOTIFS)) snapshot.getBoolean(FIELD_SILENCE_NOTIFS) else true),
                FIELD_PRIORITY_ALERTS to (if (exists && snapshot.contains(FIELD_PRIORITY_ALERTS)) snapshot.getBoolean(FIELD_PRIORITY_ALERTS) else true),
                FIELD_UPDATED_AT to serverTimestampProvider(),
            )
            firestore.collection(COLLECTION_USERS)
                .document(targetUid)
                .set(initialData, SetOptions.merge())
                .await()
        }

        if (exists) {
            val finalName = remoteName?.takeIf { it.isNotBlank() } ?: firstName
            val remoteLastName = snapshot.getString(FIELD_LAST_NAME).orEmpty()
            val finalLastName = remoteLastName.ifBlank { if (remoteName.isNullOrBlank()) lastName else "" }
            val fullName = if (finalLastName.isNotBlank()) {
                "$finalName $finalLastName".trim()
            } else {
                finalName
            }
            val role = remoteRole?.takeIf { it.isNotBlank() } ?: DEFAULT_ROLE
            val avatarUri = snapshot.getString(FIELD_AVATAR_URI)

            if (fullName.isNotBlank() || role.isNotBlank()) {
                localStore.saveProfile(
                    name = fullName.ifBlank { localStore.userName.value },
                    role = role,
                    avatarUri = avatarUri ?: localStore.avatarUri.value,
                )
            }

            // Si en Firestore aún no estaba guardado el campo email, hacemos merge para completarlo
            if (remoteEmail.isNullOrBlank() && currentUserEmail.isNotBlank()) {
                firestore.collection(COLLECTION_USERS)
                    .document(targetUid)
                    .set(
                        mapOf(
                            FIELD_EMAIL to currentUserEmail,
                            FIELD_UPDATED_AT to serverTimestampProvider(),
                        ),
                        SetOptions.merge(),
                    )
                    .await()
            }

            val dynamicBreaks = snapshot.getBoolean(FIELD_DYNAMIC_BREAKS)
            if (dynamicBreaks != null) {
                localStore.setDynamicBreaksEnabled(dynamicBreaks)
            }
            val silenceNotifs = snapshot.getBoolean(FIELD_SILENCE_NOTIFS)
            if (silenceNotifs != null) {
                localStore.setSilenceInternalNotificationsEnabled(silenceNotifs)
            }
            val priorityAlerts = snapshot.getBoolean(FIELD_PRIORITY_ALERTS)
            if (priorityAlerts != null) {
                localStore.setPriorityAlertsEnabled(priorityAlerts)
            }

            val deadlineMinutes = snapshot.getLong(FIELD_DEADLINE_MINUTES)?.toInt()
            val deadlineEnabled = snapshot.getBoolean(FIELD_DEADLINE_ENABLED)
            val focusSound = snapshot.getBoolean(FIELD_FOCUS_SOUND)
            val soundTone = snapshot.getString(FIELD_SOUND_TONE)

            val currentPrefs = localStore.getNotificationPrefs()
            localStore.saveNotificationPrefs(
                currentPrefs.copy(
                    deadlineReminderMinutes = deadlineMinutes ?: currentPrefs.deadlineReminderMinutes,
                    deadlineRemindersEnabled = deadlineEnabled ?: currentPrefs.deadlineRemindersEnabled,
                    focusStartSound = focusSound ?: currentPrefs.focusStartSound,
                    soundTone = soundTone ?: currentPrefs.soundTone,
                ),
            )
        } else {
            // El documento no existía y fue aprovisionado con los valores por defecto
            localStore.saveProfile(
                name = baseName,
                role = DEFAULT_ROLE,
                avatarUri = null,
            )
        }
    }

    private suspend fun runCatchingSafe(block: suspend () -> Unit): KronoResult<Unit> {
        return try {
            block()
            KronoResult.Success(Unit)
        } catch (t: Throwable) {
            KronoResult.Error(t)
        }
    }

    companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_TASKS = "tasks"
        const val COLLECTION_TIMER = "timer"
        const val DOCUMENT_ACTIVE_TIMER = "active"

        const val FIELD_NAME = "name"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_EMAIL = "email"
        const val FIELD_ROLE = "role"
        const val FIELD_AVATAR_URI = "avatarUri"
        const val FIELD_DEADLINE_MINUTES = "deadlineReminderMinutes"
        const val FIELD_FOCUS_SOUND = "focusStartSound"
        const val FIELD_SOUND_TONE = "soundTone"
        const val FIELD_DEADLINE_ENABLED = "deadlineRemindersEnabled"
        const val FIELD_DYNAMIC_BREAKS = "dynamicBreaksEnabled"
        const val FIELD_SILENCE_NOTIFS = "silenceInternalNotificationsEnabled"
        const val FIELD_PRIORITY_ALERTS = "priorityAlertsEnabled"
        const val FIELD_UPDATED_AT = "updatedAt"

        const val DEFAULT_NAME = "Usuario"
        const val DEFAULT_ROLE = "Estudiante y trabajador"
    }
}

package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.BlankPasswordException
import com.krono.feature.profile.domain.NetworkUnavailableException
import com.krono.feature.profile.domain.NotificationPrefs
import com.krono.feature.profile.domain.ProfileRepository
import com.krono.feature.profile.domain.SyncState
import com.krono.feature.profile.domain.syncRemoteBestEffort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [ProfileRepository] que orquesta la persistencia local reactiva ([ProfileLocalStore])
 * y la sincronización remota con Cloud Firestore y Firebase Auth ([ProfileRemoteDataSource]).
 *
 * Mantiene un estado explícito de sincronización remota ([syncState]) y garantiza tolerancia a fallos offline.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val localStore: ProfileLocalStore,
    private val remoteDataSource: ProfileRemoteDataSource,
    private val connectivityChecker: ConnectivityChecker,
) : ProfileRepository {

    constructor(localStore: ProfileLocalStore) : this(
        localStore = localStore,
        remoteDataSource = NoOpProfileRemoteDataSource(),
        connectivityChecker = ConnectivityChecker { true },
    )

    constructor() : this(
        localStore = ProfileLocalStore(),
        remoteDataSource = NoOpProfileRemoteDataSource(),
        connectivityChecker = ConnectivityChecker { true },
    )

    private val _syncState = MutableStateFlow(SyncState.CONFIRMED)

    /** Flujo observable del estado de sincronización remota hacia Firestore. */
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override val userName: StateFlow<String> get() = localStore.userName
    override val userRole: StateFlow<String> get() = localStore.userRole
    override val userEmail: StateFlow<String> get() = localStore.userEmail
    override val avatarUri: StateFlow<String?> get() = localStore.avatarUri

    override val dynamicBreaksEnabled: StateFlow<Boolean> get() = localStore.dynamicBreaksEnabled
    override val silenceInternalNotificationsEnabled: StateFlow<Boolean> get() = localStore.silenceInternalNotificationsEnabled
    override val priorityAlertsEnabled: StateFlow<Boolean> get() = localStore.priorityAlertsEnabled

    override val deadlineReminderMinutes: StateFlow<Int> get() = localStore.deadlineReminderMinutes
    override val deadlineRemindersEnabled: StateFlow<Boolean> get() = localStore.deadlineRemindersEnabled
    override val focusStartSound: StateFlow<Boolean> get() = localStore.focusStartSound
    override val soundTone: StateFlow<String> get() = localStore.soundTone

    override val activeUserId: String get() = localStore.activeUserId

    override fun isOnline(): Boolean = connectivityChecker.isOnline()

    override fun bindUser(userId: String, email: String) {
        localStore.bindUser(userId, email)
    }

    override suspend fun syncFromRemote(userId: String): KronoResult<Unit> {
        val result = remoteDataSource.syncFromRemote(userId, localStore)
        handleSyncResult(result)
        return result
    }

    override suspend fun updateProfile(name: String, role: String, avatarUri: String?): KronoResult<Unit> {
        val trimmedName = name.trim()
        val trimmedRole = role.trim()
        localStore.saveProfile(trimmedName, trimmedRole, avatarUri)

        val syncResult = syncRemoteBestEffort(OPERATION_SAVE_PROFILE) {
            remoteDataSource.saveUserProfile(
                userId = localStore.activeUserId,
                name = trimmedName,
                role = trimmedRole,
                avatarUri = avatarUri,
            )
        }
        handleSyncResult(syncResult)
        return KronoResult.Success(Unit)
    }

    override fun saveAvatarUri(uri: String?) {
        localStore.saveAvatarUri(uri)
    }

    override fun setDynamicBreaksEnabled(enabled: Boolean) {
        localStore.setDynamicBreaksEnabled(enabled)
    }

    override fun setSilenceInternalNotificationsEnabled(enabled: Boolean) {
        localStore.setSilenceInternalNotificationsEnabled(enabled)
    }

    override fun setPriorityAlertsEnabled(enabled: Boolean) {
        localStore.setPriorityAlertsEnabled(enabled)
    }

    override suspend fun syncSettingsToggles(): KronoResult<Unit> {
        val syncResult = syncRemoteBestEffort(OPERATION_SYNC_TOGGLES) {
            remoteDataSource.saveSettingsToggles(
                userId = localStore.activeUserId,
                dynamicBreaks = localStore.dynamicBreaksEnabled.value,
                silenceNotifications = localStore.silenceInternalNotificationsEnabled.value,
                priorityAlerts = localStore.priorityAlertsEnabled.value,
            )
        }
        handleSyncResult(syncResult)
        return syncResult
    }

    override suspend fun saveNotificationPrefs(prefs: NotificationPrefs): KronoResult<Unit> {
        localStore.saveNotificationPrefs(prefs)
        val syncResult = syncRemoteBestEffort(OPERATION_SAVE_PREFS) {
            remoteDataSource.saveNotificationPrefs(
                userId = localStore.activeUserId,
                prefs = prefs,
            )
        }
        handleSyncResult(syncResult)
        return KronoResult.Success(Unit)
    }

    override suspend fun resetNotificationPrefs(): KronoResult<NotificationPrefs> {
        val defaults = NotificationPrefs()
        localStore.saveNotificationPrefs(defaults)
        val syncResult = syncRemoteBestEffort(OPERATION_RESET_PREFS) {
            remoteDataSource.saveNotificationPrefs(
                userId = localStore.activeUserId,
                prefs = defaults,
            )
        }
        handleSyncResult(syncResult)
        return KronoResult.Success(defaults)
    }

    override fun getNotificationPrefs(): NotificationPrefs = localStore.getNotificationPrefs()

    override suspend fun retrySync(): KronoResult<Unit> {
        if (!connectivityChecker.isOnline()) {
            _syncState.value = SyncState.PENDING_SYNC
            return KronoResult.Error(NetworkUnavailableException())
        }

        val uid = localStore.activeUserId

        val profileResult = remoteDataSource.saveUserProfile(
            userId = uid,
            name = localStore.userName.value,
            role = localStore.userRole.value,
            avatarUri = localStore.avatarUri.value,
        )
        if (profileResult is KronoResult.Error) {
            _syncState.value = SyncState.REJECTED
            return profileResult
        }

        val togglesResult = remoteDataSource.saveSettingsToggles(
            userId = uid,
            dynamicBreaks = localStore.dynamicBreaksEnabled.value,
            silenceNotifications = localStore.silenceInternalNotificationsEnabled.value,
            priorityAlerts = localStore.priorityAlertsEnabled.value,
        )
        if (togglesResult is KronoResult.Error) {
            _syncState.value = SyncState.REJECTED
            return togglesResult
        }

        val notifsResult = remoteDataSource.saveNotificationPrefs(
            userId = uid,
            prefs = localStore.getNotificationPrefs(),
        )
        if (notifsResult is KronoResult.Error) {
            _syncState.value = SyncState.REJECTED
            return notifsResult
        }

        _syncState.value = SyncState.CONFIRMED
        return KronoResult.Success(Unit)
    }

    override suspend fun resetUserData(): KronoResult<Unit> {
        if (!connectivityChecker.isOnline()) return KronoResult.Error(NetworkUnavailableException())
        val result = remoteDataSource.resetUserData(localStore.activeUserId)
        if (result is KronoResult.Error) return result
        localStore.resetToDefaults()
        _syncState.value = SyncState.CONFIRMED
        return KronoResult.Success(Unit)
    }

    override suspend fun reauthenticate(password: String): KronoResult<Unit> {
        if (password.isBlank()) return KronoResult.Error(BlankPasswordException())
        if (!connectivityChecker.isOnline()) return KronoResult.Error(NetworkUnavailableException())
        return remoteDataSource.reauthenticate(password)
    }

    override suspend fun deleteAccount(password: String): KronoResult<Unit> {
        if (password.isBlank()) return KronoResult.Error(BlankPasswordException())
        if (!connectivityChecker.isOnline()) return KronoResult.Error(NetworkUnavailableException())

        val reauth = remoteDataSource.reauthenticate(password)
        if (reauth is KronoResult.Error) return reauth

        val deletion = remoteDataSource.deleteAccount(localStore.activeUserId)
        if (deletion is KronoResult.Error) return deletion

        localStore.resetToDefaults()
        _syncState.value = SyncState.CONFIRMED
        return KronoResult.Success(Unit)
    }

    override fun resetToDefaults() {
        localStore.resetToDefaults()
    }

    private fun handleSyncResult(result: KronoResult<Unit>) {
        when (result) {
            is KronoResult.Success -> {
                _syncState.value = if (connectivityChecker.isOnline()) SyncState.CONFIRMED else SyncState.PENDING_SYNC
            }
            is KronoResult.Error -> {
                if (result.throwable is NetworkUnavailableException) {
                    _syncState.value = SyncState.PENDING_SYNC
                } else {
                    _syncState.value = SyncState.REJECTED
                }
            }
            is KronoResult.Loading -> Unit
        }
    }

    private companion object {
        const val OPERATION_SAVE_PROFILE = "guardar perfil"
        const val OPERATION_SYNC_TOGGLES = "guardar ajustes"
        const val OPERATION_SAVE_PREFS = "guardar preferencias de notificación"
        const val OPERATION_RESET_PREFS = "restablecer preferencias de notificación"
    }
}

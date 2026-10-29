package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs

/**
 * Implementación no-op de [ProfileRemoteDataSource] para pruebas unitarias sin emulador ni red.
 */
open class NoOpProfileRemoteDataSource(
    /** Registro ordenado de las operaciones invocadas, para verificar la secuencia de borrado. */
    val calls: MutableList<String> = mutableListOf(),
) : ProfileRemoteDataSource {
    var lastSavedName: String? = null
    var lastSavedRole: String? = null
    var lastSavedAvatarUri: String? = null
    var lastSavedPrefs: NotificationPrefs? = null
    var resetUserDataCalled = false
    var deleteAccountCalled = false
    var lastDeletePassword: String? = null
    var clearActiveTimerCalled = false
    var reauthenticateFailure: Throwable? = null
    var clearActiveTimerFailure: Throwable? = null
    var syncFromRemoteCalled = false
    var lastSyncUserId: String? = null
    var shouldFail: Throwable? = null

    override suspend fun saveUserProfile(
        userId: String,
        name: String,
        role: String,
        avatarUri: String?,
    ): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        lastSavedName = name
        lastSavedRole = role
        lastSavedAvatarUri = avatarUri
        return KronoResult.Success(Unit)
    }

    override suspend fun saveNotificationPrefs(
        userId: String,
        prefs: NotificationPrefs,
    ): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        lastSavedPrefs = prefs
        return KronoResult.Success(Unit)
    }

    override suspend fun saveSettingsToggles(
        userId: String,
        dynamicBreaks: Boolean,
        silenceNotifications: Boolean,
        priorityAlerts: Boolean,
    ): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        return KronoResult.Success(Unit)
    }

    override suspend fun resetUserData(userId: String): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        calls += CALL_RESET_USER_DATA
        resetUserDataCalled = true
        return KronoResult.Success(Unit)
    }

    override suspend fun reauthenticate(password: String): KronoResult<Unit> {
        calls += CALL_REAUTHENTICATE
        reauthenticateFailure?.let { return KronoResult.Error(it) }
        lastDeletePassword = password
        return KronoResult.Success(Unit)
    }

    override suspend fun clearActiveTimer(userId: String): KronoResult<Unit> {
        calls += CALL_CLEAR_ACTIVE_TIMER
        clearActiveTimerFailure?.let { return KronoResult.Error(it) }
        clearActiveTimerCalled = true
        return KronoResult.Success(Unit)
    }

    override suspend fun deleteAccount(): KronoResult<Unit> {
        calls += CALL_DELETE_ACCOUNT
        shouldFail?.let { return KronoResult.Error(it) }
        deleteAccountCalled = true
        return KronoResult.Success(Unit)
    }

    override suspend fun syncFromRemote(
        userId: String,
        localStore: ProfileLocalStore,
    ): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        syncFromRemoteCalled = true
        lastSyncUserId = userId
        return KronoResult.Success(Unit)
    }

    companion object {
        const val CALL_REAUTHENTICATE = "reauthenticate"
        const val CALL_CLEAR_ACTIVE_TIMER = "clearActiveTimer"
        const val CALL_RESET_USER_DATA = "resetUserData"
        const val CALL_DELETE_ACCOUNT = "deleteAccount"
    }
}

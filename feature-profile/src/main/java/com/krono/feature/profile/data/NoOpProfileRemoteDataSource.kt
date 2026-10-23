package com.krono.feature.profile.data

import com.krono.core.common.KronoResult
import com.krono.feature.profile.domain.NotificationPrefs

/**
 * Implementación no-op de [ProfileRemoteDataSource] para pruebas unitarias sin emulador ni red.
 */
open class NoOpProfileRemoteDataSource : ProfileRemoteDataSource {
    var lastSavedName: String? = null
    var lastSavedRole: String? = null
    var lastSavedAvatarUri: String? = null
    var lastSavedPrefs: NotificationPrefs? = null
    var resetUserDataCalled = false
    var deleteAccountCalled = false
    var lastDeletePassword: String? = null
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
        resetUserDataCalled = true
        return KronoResult.Success(Unit)
    }

    override suspend fun deleteAccount(password: String): KronoResult<Unit> {
        shouldFail?.let { return KronoResult.Error(it) }
        deleteAccountCalled = true
        lastDeletePassword = password
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
}

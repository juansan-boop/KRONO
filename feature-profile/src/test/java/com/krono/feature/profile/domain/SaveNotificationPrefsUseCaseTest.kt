package com.krono.feature.profile.domain

import com.krono.core.common.KronoResult
import com.krono.feature.profile.data.NoOpProfileRemoteDataSource
import com.krono.feature.profile.data.ProfileLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de [SaveNotificationPrefsUseCase]: errores de validación tipados (F-15 CA-2). */
class SaveNotificationPrefsUseCaseTest {

    private val remote = NoOpProfileRemoteDataSource()
    private val useCase = SaveNotificationPrefsUseCase(ProfileLocalStore(), remote)

    @Test
    fun minutosNoPositivosDevuelvenInvalidReminderMinutesExceptionSinPersistir() = runTest {
        val result = useCase(NotificationPrefs(deadlineReminderMinutes = 0))

        assertTrue((result as KronoResult.Error).throwable is InvalidReminderMinutesException)
        assertNull(remote.lastSavedPrefs)
    }

    @Test
    fun minutosValidosSePersisten() = runTest {
        val result = useCase(NotificationPrefs(deadlineReminderMinutes = 15))

        assertTrue(result is KronoResult.Success)
    }
}

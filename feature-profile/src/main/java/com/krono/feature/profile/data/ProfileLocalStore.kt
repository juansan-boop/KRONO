package com.krono.feature.profile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.krono.feature.profile.domain.NotificationPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "krono_profile_preferences")

/**
 * Almacén local persistente para las preferencias de perfil, ajustes del sistema
 * y notificaciones (F-19 CA-2, F-20 CA-2, F-15 CA-2, F-21 CA-1).
 *
 * Migrado a [DataStore] de preferencias con aislamiento estricto por identificador de usuario (`uid`),
 * lecturas reactivas no bloqueantes mediante [StateFlow] y persistencia asíncrona segura.
 *
 * @param dataStore Instancia opcional de [DataStore] para persistencia física en disco.
 * @param coroutineScope Ámbito para la recolección reactiva y persistencia asíncrona.
 * @param ioDispatcher Despachador de corrutinas para operaciones de entrada/salida.
 */
@Singleton
class ProfileLocalStore internal constructor(
    private val dataStore: DataStore<Preferences>?,
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        dataStore = try {
            context.profileDataStore
        } catch (_: Exception) {
            null
        },
        coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ioDispatcher = Dispatchers.IO,
    )

    constructor() : this(
        dataStore = null,
        coroutineScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private var currentUserId: String = ""

    /**
     * Identificador del usuario actualmente vinculado en el almacén local.
     */
    val activeUserId: String get() = currentUserId

    private val inMemoryValues = mutableMapOf<String, Any?>()
    private var dataStoreCollectorJob: Job? = null

    // Datos de perfil (F-19)
    private val _userName = MutableStateFlow(DEFAULT_NAME)

    /** Nombre visible del usuario activo. */
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userRole = MutableStateFlow(DEFAULT_ROLE)

    /** Ocupación o rol principal del usuario activo. */
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userEmail = MutableStateFlow(DEFAULT_EMAIL)

    /** Correo electrónico autenticado del usuario activo. */
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _avatarUri = MutableStateFlow<String?>(null)

    /** URI o ruta local del avatar seleccionado por el usuario. */
    val avatarUri: StateFlow<String?> = _avatarUri.asStateFlow()

    // Interruptores de ajustes (F-20 CA-2)
    private val _dynamicBreaksEnabled = MutableStateFlow(true)

    /** Indica si las sugerencias dinámicas de descansos están habilitadas. */
    val dynamicBreaksEnabled: StateFlow<Boolean> = _dynamicBreaksEnabled.asStateFlow()

    private val _silenceInternalNotificationsEnabled = MutableStateFlow(true)

    /** Indica si las notificaciones no esenciales se silencian durante el enfoque. */
    val silenceInternalNotificationsEnabled: StateFlow<Boolean> = _silenceInternalNotificationsEnabled.asStateFlow()

    private val _priorityAlertsEnabled = MutableStateFlow(true)

    /** Indica si las alertas de tareas de prioridad crítica P1 están activadas. */
    val priorityAlertsEnabled: StateFlow<Boolean> = _priorityAlertsEnabled.asStateFlow()

    // Preferencias de notificación (F-15 CA-2)
    private val _deadlineReminderMinutes = MutableStateFlow(NotificationPrefs.DEFAULT_REMINDER_MINUTES)

    /** Minutos de anticipación configurados para las alarmas de vencimiento. */
    val deadlineReminderMinutes: StateFlow<Int> = _deadlineReminderMinutes.asStateFlow()

    private val _deadlineRemindersEnabled = MutableStateFlow(true)

    /** Indica si los recordatorios de fecha límite están activados. */
    val deadlineRemindersEnabled: StateFlow<Boolean> = _deadlineRemindersEnabled.asStateFlow()

    private val _focusStartSound = MutableStateFlow(true)

    /** Indica si se emite sonido de confirmación al iniciar un bloque de concentración. */
    val focusStartSound: StateFlow<Boolean> = _focusStartSound.asStateFlow()

    private val _soundTone = MutableStateFlow(NotificationPrefs.DEFAULT_SOUND_TONE)

    /** Tono de notificación seleccionado por el usuario. */
    val soundTone: StateFlow<String> = _soundTone.asStateFlow()

    /**
     * Vincula el almacenamiento local con la sesión activa de Firebase y su identificador (`uid`).
     * Aplica aislamiento estricto por usuario y restaura o deriva el perfil correspondiente.
     *
     * @param userId Identificador único del usuario autenticado en Firebase.
     * @param email Correo electrónico registrado para el usuario.
     */
    fun bindUser(userId: String, email: String) {
        currentUserId = userId
        _userEmail.value = email

        val savedName = inMemoryValues[userKey(KEY_USER_NAME)] as? String
        if (!savedName.isNullOrBlank()) {
            _userName.value = savedName
        } else {
            val derived = formatNameFromEmail(email)
            _userName.value = derived.ifBlank { DEFAULT_NAME }
        }

        _userRole.value = (inMemoryValues[userKey(KEY_USER_ROLE)] as? String) ?: DEFAULT_ROLE
        _avatarUri.value = inMemoryValues[userKey(KEY_AVATAR_URI)] as? String

        _dynamicBreaksEnabled.value = (inMemoryValues[userKey(KEY_DYNAMIC_BREAKS)] as? Boolean) ?: true
        _silenceInternalNotificationsEnabled.value = (inMemoryValues[userKey(KEY_SILENCE_NOTIFS)] as? Boolean) ?: true
        _priorityAlertsEnabled.value = (inMemoryValues[userKey(KEY_PRIORITY_ALERTS)] as? Boolean) ?: true

        _deadlineReminderMinutes.value = (inMemoryValues[userKey(KEY_DEADLINE_MINUTES)] as? Int) ?: NotificationPrefs.DEFAULT_REMINDER_MINUTES
        _deadlineRemindersEnabled.value = (inMemoryValues[userKey(KEY_DEADLINE_ENABLED)] as? Boolean) ?: true
        _focusStartSound.value = (inMemoryValues[userKey(KEY_FOCUS_SOUND)] as? Boolean) ?: true
        _soundTone.value = (inMemoryValues[userKey(KEY_SOUND_TONE)] as? String) ?: NotificationPrefs.DEFAULT_SOUND_TONE

        dataStoreCollectorJob?.cancel()
        if (dataStore != null) {
            val targetUserId = userId
            dataStoreCollectorJob = coroutineScope.launch(ioDispatcher) {
                dataStore.data
                    .catch { emit(emptyPreferences()) }
                    .collect { preferences ->
                        if (currentUserId != targetUserId) return@collect

                        val keyName = stringPreferencesKey(userKey(KEY_USER_NAME))
                        val keyRole = stringPreferencesKey(userKey(KEY_USER_ROLE))
                        val keyAvatar = stringPreferencesKey(userKey(KEY_AVATAR_URI))
                        val keyBreaks = booleanPreferencesKey(userKey(KEY_DYNAMIC_BREAKS))
                        val keySilence = booleanPreferencesKey(userKey(KEY_SILENCE_NOTIFS))
                        val keyAlerts = booleanPreferencesKey(userKey(KEY_PRIORITY_ALERTS))
                        val keyDeadlineMinutes = intPreferencesKey(userKey(KEY_DEADLINE_MINUTES))
                        val keyDeadlineEnabled = booleanPreferencesKey(userKey(KEY_DEADLINE_ENABLED))
                        val keyFocusSound = booleanPreferencesKey(userKey(KEY_FOCUS_SOUND))
                        val keySoundTone = stringPreferencesKey(userKey(KEY_SOUND_TONE))

                        val storedName = preferences[keyName]
                        if (!storedName.isNullOrBlank()) {
                            _userName.value = storedName
                            inMemoryValues[userKey(KEY_USER_NAME)] = storedName
                        } else {
                            val derived = formatNameFromEmail(_userEmail.value).ifBlank { DEFAULT_NAME }
                            _userName.value = derived
                        }

                        val storedRole = preferences[keyRole] ?: DEFAULT_ROLE
                        _userRole.value = storedRole
                        inMemoryValues[userKey(KEY_USER_ROLE)] = storedRole

                        val storedAvatar = preferences[keyAvatar]
                        _avatarUri.value = storedAvatar
                        inMemoryValues[userKey(KEY_AVATAR_URI)] = storedAvatar

                        val storedBreaks = preferences[keyBreaks] ?: true
                        _dynamicBreaksEnabled.value = storedBreaks
                        inMemoryValues[userKey(KEY_DYNAMIC_BREAKS)] = storedBreaks

                        val storedSilence = preferences[keySilence] ?: true
                        _silenceInternalNotificationsEnabled.value = storedSilence
                        inMemoryValues[userKey(KEY_SILENCE_NOTIFS)] = storedSilence

                        val storedAlerts = preferences[keyAlerts] ?: true
                        _priorityAlertsEnabled.value = storedAlerts
                        inMemoryValues[userKey(KEY_PRIORITY_ALERTS)] = storedAlerts

                        val storedDeadlineMinutes = preferences[keyDeadlineMinutes] ?: NotificationPrefs.DEFAULT_REMINDER_MINUTES
                        _deadlineReminderMinutes.value = storedDeadlineMinutes
                        inMemoryValues[userKey(KEY_DEADLINE_MINUTES)] = storedDeadlineMinutes

                        val storedDeadlineEnabled = preferences[keyDeadlineEnabled] ?: true
                        _deadlineRemindersEnabled.value = storedDeadlineEnabled
                        inMemoryValues[userKey(KEY_DEADLINE_ENABLED)] = storedDeadlineEnabled

                        val storedFocusSound = preferences[keyFocusSound] ?: true
                        _focusStartSound.value = storedFocusSound
                        inMemoryValues[userKey(KEY_FOCUS_SOUND)] = storedFocusSound

                        val storedSoundTone = preferences[keySoundTone] ?: NotificationPrefs.DEFAULT_SOUND_TONE
                        _soundTone.value = storedSoundTone
                        inMemoryValues[userKey(KEY_SOUND_TONE)] = storedSoundTone
                    }
            }
        }
    }

    /**
     * Guarda el nombre, rol y avatar del perfil del usuario (F-19 CA-2).
     *
     * @param name Nombre visible del perfil.
     * @param role Cargo o rol principal.
     * @param avatarUri Identificador URI o ruta de la foto seleccionada.
     */
    fun saveProfile(name: String, role: String, avatarUri: String? = _avatarUri.value) {
        _userName.value = name
        _userRole.value = role
        _avatarUri.value = avatarUri

        inMemoryValues[userKey(KEY_USER_NAME)] = name
        inMemoryValues[userKey(KEY_USER_ROLE)] = role
        if (avatarUri != null) {
            inMemoryValues[userKey(KEY_AVATAR_URI)] = avatarUri
        } else {
            inMemoryValues.remove(userKey(KEY_AVATAR_URI))
        }

        persistAsync { preferences ->
            preferences[stringPreferencesKey(userKey(KEY_USER_NAME))] = name
            preferences[stringPreferencesKey(userKey(KEY_USER_ROLE))] = role
            val keyAvatar = stringPreferencesKey(userKey(KEY_AVATAR_URI))
            if (avatarUri != null) {
                preferences[keyAvatar] = avatarUri
            } else {
                preferences.remove(keyAvatar)
            }
        }
    }

    /**
     * Persiste el URI de la foto de perfil seleccionada.
     *
     * @param uri URI como cadena de texto, o `null` si se descarta el avatar.
     */
    fun saveAvatarUri(uri: String?) {
        _avatarUri.value = uri
        if (uri != null) {
            inMemoryValues[userKey(KEY_AVATAR_URI)] = uri
        } else {
            inMemoryValues.remove(userKey(KEY_AVATAR_URI))
        }

        persistAsync { preferences ->
            val keyAvatar = stringPreferencesKey(userKey(KEY_AVATAR_URI))
            if (uri != null) {
                preferences[keyAvatar] = uri
            } else {
                preferences.remove(keyAvatar)
            }
        }
    }

    /**
     * Persiste el interruptor de sugerencia dinámica de descansos (F-20 CA-2).
     *
     * @param enabled Estado del interruptor.
     */
    fun setDynamicBreaksEnabled(enabled: Boolean) {
        _dynamicBreaksEnabled.value = enabled
        inMemoryValues[userKey(KEY_DYNAMIC_BREAKS)] = enabled
        persistAsync { preferences ->
            preferences[booleanPreferencesKey(userKey(KEY_DYNAMIC_BREAKS))] = enabled
        }
    }

    /**
     * Persiste el interruptor de silenciar notificaciones internas (F-20 CA-2).
     *
     * @param enabled Estado del interruptor.
     */
    fun setSilenceInternalNotificationsEnabled(enabled: Boolean) {
        _silenceInternalNotificationsEnabled.value = enabled
        inMemoryValues[userKey(KEY_SILENCE_NOTIFS)] = enabled
        persistAsync { preferences ->
            preferences[booleanPreferencesKey(userKey(KEY_SILENCE_NOTIFS))] = enabled
        }
    }

    /**
     * Persiste el interruptor de alertas prioritarias P1 (F-20 CA-2).
     *
     * @param enabled Estado del interruptor.
     */
    fun setPriorityAlertsEnabled(enabled: Boolean) {
        _priorityAlertsEnabled.value = enabled
        inMemoryValues[userKey(KEY_PRIORITY_ALERTS)] = enabled
        persistAsync { preferences ->
            preferences[booleanPreferencesKey(userKey(KEY_PRIORITY_ALERTS))] = enabled
        }
    }

    /**
     * Guarda y persiste las preferencias de notificación y alarmas (F-15 CA-2).
     *
     * @param prefs Modelo de datos con la configuración de notificaciones y tonos.
     */
    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        _deadlineReminderMinutes.value = prefs.deadlineReminderMinutes
        _deadlineRemindersEnabled.value = prefs.deadlineRemindersEnabled
        _focusStartSound.value = prefs.focusStartSound
        _soundTone.value = prefs.soundTone

        inMemoryValues[userKey(KEY_DEADLINE_MINUTES)] = prefs.deadlineReminderMinutes
        inMemoryValues[userKey(KEY_DEADLINE_ENABLED)] = prefs.deadlineRemindersEnabled
        inMemoryValues[userKey(KEY_FOCUS_SOUND)] = prefs.focusStartSound
        inMemoryValues[userKey(KEY_SOUND_TONE)] = prefs.soundTone

        persistAsync { preferences ->
            preferences[intPreferencesKey(userKey(KEY_DEADLINE_MINUTES))] = prefs.deadlineReminderMinutes
            preferences[booleanPreferencesKey(userKey(KEY_DEADLINE_ENABLED))] = prefs.deadlineRemindersEnabled
            preferences[booleanPreferencesKey(userKey(KEY_FOCUS_SOUND))] = prefs.focusStartSound
            preferences[stringPreferencesKey(userKey(KEY_SOUND_TONE))] = prefs.soundTone
        }
    }

    /**
     * Obtiene el modelo actual de preferencias de notificación en memoria.
     *
     * @return Instancia inmutable de [NotificationPrefs].
     */
    fun getNotificationPrefs(): NotificationPrefs {
        return NotificationPrefs(
            deadlineReminderMinutes = _deadlineReminderMinutes.value,
            deadlineRemindersEnabled = _deadlineRemindersEnabled.value,
            focusStartSound = _focusStartSound.value,
            soundTone = _soundTone.value,
        )
    }

    /**
     * Restablece los datos de perfil, ajustes y notificaciones a valores de fábrica para el usuario actual (F-21 CA-1).
     */
    fun resetToDefaults() {
        val defaultNameForUser = formatNameFromEmail(_userEmail.value).ifBlank { DEFAULT_NAME }
        _userName.value = defaultNameForUser
        _userRole.value = DEFAULT_ROLE
        _avatarUri.value = null
        _dynamicBreaksEnabled.value = true
        _silenceInternalNotificationsEnabled.value = true
        _priorityAlertsEnabled.value = true
        _deadlineReminderMinutes.value = NotificationPrefs.DEFAULT_REMINDER_MINUTES
        _deadlineRemindersEnabled.value = true
        _focusStartSound.value = true
        _soundTone.value = NotificationPrefs.DEFAULT_SOUND_TONE

        inMemoryValues.remove(userKey(KEY_USER_NAME))
        inMemoryValues.remove(userKey(KEY_USER_ROLE))
        inMemoryValues.remove(userKey(KEY_AVATAR_URI))
        inMemoryValues.remove(userKey(KEY_DYNAMIC_BREAKS))
        inMemoryValues.remove(userKey(KEY_SILENCE_NOTIFS))
        inMemoryValues.remove(userKey(KEY_PRIORITY_ALERTS))
        inMemoryValues.remove(userKey(KEY_DEADLINE_MINUTES))
        inMemoryValues.remove(userKey(KEY_DEADLINE_ENABLED))
        inMemoryValues.remove(userKey(KEY_FOCUS_SOUND))
        inMemoryValues.remove(userKey(KEY_SOUND_TONE))

        persistAsync { preferences ->
            preferences.remove(stringPreferencesKey(userKey(KEY_USER_NAME)))
            preferences.remove(stringPreferencesKey(userKey(KEY_USER_ROLE)))
            preferences.remove(stringPreferencesKey(userKey(KEY_AVATAR_URI)))
            preferences.remove(booleanPreferencesKey(userKey(KEY_DYNAMIC_BREAKS)))
            preferences.remove(booleanPreferencesKey(userKey(KEY_SILENCE_NOTIFS)))
            preferences.remove(booleanPreferencesKey(userKey(KEY_PRIORITY_ALERTS)))
            preferences.remove(intPreferencesKey(userKey(KEY_DEADLINE_MINUTES)))
            preferences.remove(booleanPreferencesKey(userKey(KEY_DEADLINE_ENABLED)))
            preferences.remove(booleanPreferencesKey(userKey(KEY_FOCUS_SOUND)))
            preferences.remove(stringPreferencesKey(userKey(KEY_SOUND_TONE)))
        }
    }

    private fun persistAsync(transform: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        if (dataStore != null) {
            coroutineScope.launch(ioDispatcher) {
                try {
                    dataStore.edit { preferences ->
                        transform(preferences)
                    }
                } catch (_: Exception) {
                    // Silenciamos fallos de I/O en la persistencia local para no degradar la experiencia de usuario
                }
            }
        }
    }

    private fun userKey(key: String): String =
        if (currentUserId.isNotBlank()) "${currentUserId}_$key" else key

    companion object {
        const val DEFAULT_NAME = "Usuario"
        const val DEFAULT_ROLE = "Estudiante y trabajador"
        const val DEFAULT_EMAIL = ""

        val AVAILABLE_ROLES = listOf(
            "Estudiante y trabajador",
            "Estudiante",
            "Trabajador tiempo completo",
            "Trabajador tiempo parcial",
        )

        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_AVATAR_URI = "key_avatar_uri"
        private const val KEY_DYNAMIC_BREAKS = "key_dynamic_breaks"
        private const val KEY_SILENCE_NOTIFS = "key_silence_notifs"
        private const val KEY_PRIORITY_ALERTS = "key_priority_alerts"
        private const val KEY_DEADLINE_MINUTES = "key_deadline_minutes"
        private const val KEY_DEADLINE_ENABLED = "key_deadline_enabled"
        private const val KEY_FOCUS_SOUND = "key_focus_sound"
        private const val KEY_SOUND_TONE = "key_sound_tone"

        /**
         * Formatea la parte local de un correo antes del '@' (ej. 'juan.esteban' -> 'Juan Esteban').
         *
         * @param email Correo electrónico a partir del cual se deriva el nombre legible.
         * @return Nombre derivado en formato de mayúscula inicial por palabra.
         */
        fun formatNameFromEmail(email: String): String {
            val localPart = email.substringBefore('@').trim()
            if (localPart.isBlank()) return ""
            return localPart
                .replace(Regex("[._\\-]+"), " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase() else char.toString()
                    }
                }
        }
    }
}

package com.krono.feature.profile.data

import android.content.Context
import android.content.SharedPreferences
import com.krono.feature.profile.domain.NotificationPrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Almacén local persistente para las preferencias de perfil, ajustes del sistema
 * y notificaciones (F-19 CA-2, F-20 CA-2, F-15 CA-2, F-21 CA-1).
 *
 * Persiste los datos en [SharedPreferences] asociados al identificador del usuario activo
 * y mantiene una copia reactiva observable en memoria con [StateFlow].
 */
@Singleton
class ProfileLocalStore internal constructor(
    private val prefs: SharedPreferences?,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        },
    )

    constructor() : this(null as SharedPreferences?)

    private var currentUserId: String = ""
    val activeUserId: String get() = currentUserId

    // Datos de perfil (F-19)
    private val _userName = MutableStateFlow(loadString(userKey(KEY_USER_NAME), DEFAULT_NAME))
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userRole = MutableStateFlow(loadString(userKey(KEY_USER_ROLE), DEFAULT_ROLE))
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _userEmail = MutableStateFlow(loadString(userKey(KEY_USER_EMAIL), DEFAULT_EMAIL))
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _avatarUri = MutableStateFlow<String?>(loadNullableString(userKey(KEY_AVATAR_URI)))
    val avatarUri: StateFlow<String?> = _avatarUri.asStateFlow()

    // Interruptores de ajustes (F-20 CA-2)
    private val _dynamicBreaksEnabled = MutableStateFlow(loadBoolean(userKey(KEY_DYNAMIC_BREAKS), true))
    val dynamicBreaksEnabled: StateFlow<Boolean> = _dynamicBreaksEnabled.asStateFlow()

    private val _silenceInternalNotificationsEnabled = MutableStateFlow(loadBoolean(userKey(KEY_SILENCE_NOTIFS), true))
    val silenceInternalNotificationsEnabled: StateFlow<Boolean> = _silenceInternalNotificationsEnabled.asStateFlow()

    private val _priorityAlertsEnabled = MutableStateFlow(loadBoolean(userKey(KEY_PRIORITY_ALERTS), true))
    val priorityAlertsEnabled: StateFlow<Boolean> = _priorityAlertsEnabled.asStateFlow()

    // Preferencias de notificación (F-15 CA-2)
    private val _deadlineReminderMinutes = MutableStateFlow(loadInt(userKey(KEY_DEADLINE_MINUTES), NotificationPrefs.DEFAULT_REMINDER_MINUTES))
    val deadlineReminderMinutes: StateFlow<Int> = _deadlineReminderMinutes.asStateFlow()

    private val _deadlineRemindersEnabled = MutableStateFlow(loadBoolean(userKey(KEY_DEADLINE_ENABLED), true))
    val deadlineRemindersEnabled: StateFlow<Boolean> = _deadlineRemindersEnabled.asStateFlow()

    private val _focusStartSound = MutableStateFlow(loadBoolean(userKey(KEY_FOCUS_SOUND), true))
    val focusStartSound: StateFlow<Boolean> = _focusStartSound.asStateFlow()

    private val _soundTone = MutableStateFlow(loadString(userKey(KEY_SOUND_TONE), NotificationPrefs.DEFAULT_SOUND_TONE))
    val soundTone: StateFlow<String> = _soundTone.asStateFlow()

    /**
     * Vincula el almacenamiento local con la sesión activa de Firebase.
     * Si el usuario no tiene nombre guardado, deriva el nombre a partir de su correo electrónico.
     */
    fun bindUser(userId: String, email: String) {
        currentUserId = userId
        _userEmail.value = email

        val savedName = prefs?.getString(userKey(KEY_USER_NAME), null)
        if (!savedName.isNullOrBlank()) {
            _userName.value = savedName
        } else {
            val derived = formatNameFromEmail(email)
            _userName.value = derived.ifBlank { DEFAULT_NAME }
        }

        _userRole.value = prefs?.getString(userKey(KEY_USER_ROLE), DEFAULT_ROLE) ?: DEFAULT_ROLE
        _avatarUri.value = prefs?.getString(userKey(KEY_AVATAR_URI), null)

        _dynamicBreaksEnabled.value = loadBoolean(userKey(KEY_DYNAMIC_BREAKS), true)
        _silenceInternalNotificationsEnabled.value = loadBoolean(userKey(KEY_SILENCE_NOTIFS), true)
        _priorityAlertsEnabled.value = loadBoolean(userKey(KEY_PRIORITY_ALERTS), true)

        _deadlineReminderMinutes.value = loadInt(userKey(KEY_DEADLINE_MINUTES), NotificationPrefs.DEFAULT_REMINDER_MINUTES)
        _deadlineRemindersEnabled.value = loadBoolean(userKey(KEY_DEADLINE_ENABLED), true)
        _focusStartSound.value = loadBoolean(userKey(KEY_FOCUS_SOUND), true)
        _soundTone.value = loadString(userKey(KEY_SOUND_TONE), NotificationPrefs.DEFAULT_SOUND_TONE)
    }

    /**
     * Guarda el nombre, rol y avatar del perfil del usuario (F-19 CA-2).
     */
    fun saveProfile(name: String, role: String, avatarUri: String? = _avatarUri.value) {
        _userName.value = name
        _userRole.value = role
        _avatarUri.value = avatarUri
        saveString(userKey(KEY_USER_NAME), name)
        saveString(userKey(KEY_USER_ROLE), role)
        if (avatarUri != null) {
            saveString(userKey(KEY_AVATAR_URI), avatarUri)
        } else {
            removeKey(userKey(KEY_AVATAR_URI))
        }
    }

    /**
     * Persiste el URI de la foto de perfil seleccionada.
     */
    fun saveAvatarUri(uri: String?) {
        _avatarUri.value = uri
        if (uri != null) {
            saveString(userKey(KEY_AVATAR_URI), uri)
        } else {
            removeKey(userKey(KEY_AVATAR_URI))
        }
    }

    /**
     * Persiste el interruptor de sugerencia dinámica de descansos (F-20 CA-2).
     */
    fun setDynamicBreaksEnabled(enabled: Boolean) {
        _dynamicBreaksEnabled.value = enabled
        saveBoolean(userKey(KEY_DYNAMIC_BREAKS), enabled)
    }

    /**
     * Persiste el interruptor de silenciar notificaciones internas (F-20 CA-2).
     */
    fun setSilenceInternalNotificationsEnabled(enabled: Boolean) {
        _silenceInternalNotificationsEnabled.value = enabled
        saveBoolean(userKey(KEY_SILENCE_NOTIFS), enabled)
    }

    /**
     * Persiste el interruptor de alertas prioritarias P1 (F-20 CA-2).
     */
    fun setPriorityAlertsEnabled(enabled: Boolean) {
        _priorityAlertsEnabled.value = enabled
        saveBoolean(userKey(KEY_PRIORITY_ALERTS), enabled)
    }

    /**
     * Guarda y persiste las preferencias de notificación y alarmas (F-15 CA-2).
     */
    fun saveNotificationPrefs(prefs: NotificationPrefs) {
        _deadlineReminderMinutes.value = prefs.deadlineReminderMinutes
        _deadlineRemindersEnabled.value = prefs.deadlineRemindersEnabled
        _focusStartSound.value = prefs.focusStartSound
        _soundTone.value = prefs.soundTone

        saveInt(userKey(KEY_DEADLINE_MINUTES), prefs.deadlineReminderMinutes)
        saveBoolean(userKey(KEY_DEADLINE_ENABLED), prefs.deadlineRemindersEnabled)
        saveBoolean(userKey(KEY_FOCUS_SOUND), prefs.focusStartSound)
        saveString(userKey(KEY_SOUND_TONE), prefs.soundTone)
    }

    /**
     * Obtiene el modelo actual de preferencias de notificación.
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

        prefs?.edit()?.apply {
            remove(userKey(KEY_USER_NAME))
            remove(userKey(KEY_USER_ROLE))
            remove(userKey(KEY_AVATAR_URI))
            remove(userKey(KEY_DYNAMIC_BREAKS))
            remove(userKey(KEY_SILENCE_NOTIFS))
            remove(userKey(KEY_PRIORITY_ALERTS))
            remove(userKey(KEY_DEADLINE_MINUTES))
            remove(userKey(KEY_DEADLINE_ENABLED))
            remove(userKey(KEY_FOCUS_SOUND))
            remove(userKey(KEY_SOUND_TONE))
        }?.apply()
    }

    private fun userKey(key: String): String =
        if (currentUserId.isNotBlank()) "${currentUserId}_$key" else key

    private fun loadString(key: String, default: String): String =
        prefs?.getString(key, default) ?: default

    private fun loadNullableString(key: String): String? =
        prefs?.getString(key, null)

    private fun loadBoolean(key: String, default: Boolean): Boolean =
        prefs?.getBoolean(key, default) ?: default

    private fun loadInt(key: String, default: Int): Int =
        prefs?.getInt(key, default) ?: default

    private fun saveString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    private fun saveBoolean(key: String, value: Boolean) {
        prefs?.edit()?.putBoolean(key, value)?.apply()
    }

    private fun saveInt(key: String, value: Int) {
        prefs?.edit()?.putInt(key, value)?.apply()
    }

    private fun removeKey(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }

    companion object {
        private const val PREFS_NAME = "krono_profile_local_store"
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
        private const val KEY_USER_EMAIL = "key_user_email"
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

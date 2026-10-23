package com.krono.feature.profile.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.krono.feature.profile.R
import com.krono.feature.profile.edit.EditProfileRoute
import com.krono.feature.profile.notifications.NotificationPreferencesRoute
import com.krono.feature.profile.security.SecurityZoneRoute
import com.krono.feature.profile.settings.SettingsEvent
import com.krono.feature.profile.settings.SettingsScreen
import com.krono.feature.profile.settings.SettingsViewModel

/**
 * Ruta canónica de la pantalla de Ajustes y Perfil (F-19, F-20, F-25 CA-4).
 */
const val SETTINGS_ROUTE = "profile/settings"

/**
 * Ruta canónica de la pantalla de Edición de Perfil (F-19 CA-2).
 */
const val EDIT_PROFILE_ROUTE = "profile/edit"

/**
 * Ruta canónica de la pantalla de Zona de Seguridad (F-21 CA-1, CA-2).
 */
const val SECURITY_ZONE_ROUTE = "profile/security"

/**
 * Ruta canónica de la pantalla de Preferencias de Notificaciones y Alarmas (F-15).
 */
const val NOTIFICATION_PREFS_ROUTE = "profile/notifications"

/**
 * Navega hacia la pantalla de Ajustes y Perfil (F-19, F-20).
 */
fun NavController.navigateToSettings(navOptions: NavOptions? = null) {
    navigate(SETTINGS_ROUTE, navOptions)
}

/**
 * Navega hacia la pantalla de Edición de Perfil (F-19 CA-2).
 */
fun NavController.navigateToEditProfile(navOptions: NavOptions? = null) {
    navigate(EDIT_PROFILE_ROUTE, navOptions)
}

/**
 * Navega hacia la pantalla de Zona de Seguridad y purga de datos (F-21).
 */
fun NavController.navigateToSecurityZone(navOptions: NavOptions? = null) {
    navigate(SECURITY_ZONE_ROUTE, navOptions)
}

/**
 * Navega hacia la pantalla de Preferencias de Notificaciones y Alarmas (F-15).
 */
fun NavController.navigateToNotificationPreferences(navOptions: NavOptions? = null) {
    navigate(NOTIFICATION_PREFS_ROUTE, navOptions)
}

/**
 * Contrato de navegación de `feature-profile`.
 *
 * Agrega el destino de Ajustes al grafo de navegación de la aplicación sin acoplar
 * el módulo con destinos de otros features ni con el módulo `:app`.
 *
 * @param onBackClick Navegación hacia atrás (ej. retorno a Dashboard).
 * @param onEditProfileClick Navegación para editar el perfil del usuario.
 * @param onFocusTemplatesClick Navegación al configurador de plantillas de concentración.
 * @param onSecurityZoneClick Navegación a la zona de seguridad y purga de datos.
 * @param onNotificationPrefsClick Navegación a la pantalla de notificaciones y alarmas (F-15).
 * @param onLoggedOut Notificación de cierre de sesión exitoso (para redirigir a Auth).
 * @param onThemeSelectorClick Acción para abrir el selector de tema visual.
 */
fun NavGraphBuilder.settingsScreen(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit = {},
    onFocusTemplatesClick: () -> Unit = {},
    onSecurityZoneClick: () -> Unit = {},
    onNotificationPrefsClick: () -> Unit = {},
    onLoggedOut: () -> Unit,
    onThemeSelectorClick: () -> Unit = {},
) {
    composable(SETTINGS_ROUTE) {
        SettingsRoute(
            onBackClick = onBackClick,
            onEditProfileClick = onEditProfileClick,
            onFocusTemplatesClick = onFocusTemplatesClick,
            onSecurityZoneClick = onSecurityZoneClick,
            onNotificationPrefsClick = onNotificationPrefsClick,
            onLoggedOut = onLoggedOut,
            onThemeSelectorClick = onThemeSelectorClick,
        )
    }
}

/**
 * Punto de entrada con estado del destino de Ajustes.
 *
 * Conecta [SettingsViewModel] con la pantalla stateless [SettingsScreen] y
 * escucha los eventos de un solo uso (como [SettingsEvent.LoggedOut]).
 */
@Composable
fun SettingsRoute(
    onBackClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {},
    onFocusTemplatesClick: () -> Unit = {},
    onSecurityZoneClick: () -> Unit = {},
    onNotificationPrefsClick: () -> Unit = {},
    onLoggedOut: () -> Unit,
    onThemeSelectorClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showFocusTemplatesDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    SettingsScreen(
        state = state,
        onBackClick = onBackClick,
        onEditProfileClick = onEditProfileClick,
        onNotificationPrefsClick = onNotificationPrefsClick,
        onDynamicBreaksToggle = viewModel::onDynamicBreaksToggle,
        onSilenceNotificationsToggle = viewModel::onSilenceNotificationsToggle,
        onFocusTemplatesClick = {
            showFocusTemplatesDialog = true
            onFocusTemplatesClick()
        },
        onPriorityAlertsToggle = viewModel::onPriorityAlertsToggle,
        onThemeSelectorClick = {
            Toast.makeText(
                context,
                context.getString(R.string.settings_theme_permanent_notice),
                Toast.LENGTH_SHORT,
            ).show()
            onThemeSelectorClick()
        },
        onSecurityZoneClick = onSecurityZoneClick,
        onRequestLogout = viewModel::onRequestLogout,
        onDismissLogoutDialog = viewModel::onDismissLogoutDialog,
        onConfirmLogout = viewModel::onConfirmLogout,
        showFocusTemplatesDialog = showFocusTemplatesDialog,
        onDismissFocusTemplatesDialog = { showFocusTemplatesDialog = false },
    )
}

/**
 * Agrega el destino de Edición de Perfil (F-19 CA-2) al grafo de navegación.
 *
 * @param onBackClick Acción al presionar atrás o cancelar la edición.
 * @param onProfileSaved Acción al completar y guardar los cambios exitosamente (por defecto regresa atrás).
 */
fun NavGraphBuilder.editProfileScreen(
    onBackClick: () -> Unit,
    onProfileSaved: () -> Unit = onBackClick,
) {
    composable(EDIT_PROFILE_ROUTE) {
        EditProfileRoute(
            onBackClick = onBackClick,
            onProfileSaved = onProfileSaved,
        )
    }
}

/**
 * Agrega el destino de Zona de Seguridad y purga de datos (F-21 CA-1, CA-2) al grafo de navegación.
 *
 * @param onBackClick Navegación hacia atrás (retorno al Hub de Ajustes).
 * @param onAccountDeleted Navegación tras eliminar la cuenta (retorno a pantalla de Auth).
 */
fun NavGraphBuilder.securityZoneScreen(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    composable(SECURITY_ZONE_ROUTE) {
        SecurityZoneRoute(
            onBackClick = onBackClick,
            onAccountDeleted = onAccountDeleted,
        )
    }
}

/**
 * Agrega el destino de Notificaciones y Alarmas (F-15 CA-1, CA-2, CA-3) al grafo de navegación.
 *
 * @param onBackClick Navegación hacia atrás (retorno al Hub de Ajustes).
 */
fun NavGraphBuilder.notificationPreferencesScreen(
    onBackClick: () -> Unit,
) {
    composable(NOTIFICATION_PREFS_ROUTE) {
        NotificationPreferencesRoute(
            onBackClick = onBackClick,
        )
    }
}




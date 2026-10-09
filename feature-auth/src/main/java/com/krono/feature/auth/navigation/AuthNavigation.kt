package com.krono.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.krono.feature.auth.forgot.ForgotPasswordRoute
import com.krono.feature.auth.login.LoginRoute
import com.krono.feature.auth.register.RegisterRoute

/** Ruta del grafo de autenticación. `app` la usa como destino inicial cuando no hay sesión. */
const val AUTH_GRAPH_ROUTE = "auth"

/** Rutas internas del grafo de autenticación. */
internal object AuthRoutes {
    /** Pantalla de iniciar sesión. */
    const val LOGIN = "auth/login"
    /** Pantalla de crear cuenta. */
    const val REGISTER = "auth/register"
    /** Pantalla de recuperar contraseña. */
    const val FORGOT_PASSWORD = "auth/forgot-password"
}

/**
 * Grafo de autenticación (login, registro y recuperación). No conoce otros
 * feature-*: al autenticarse llama a [onAuthSuccess] y `app` decide a dónde ir.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthSuccess: () -> Unit,
) {
    navigation(startDestination = AuthRoutes.LOGIN, route = AUTH_GRAPH_ROUTE) {
        composable(AuthRoutes.LOGIN) {
            LoginRoute(
                onLoggedIn = onAuthSuccess,
                onForgotPassword = { navController.navigate(AuthRoutes.FORGOT_PASSWORD) { launchSingleTop = true } },
                onCreateAccount = { navController.navigate(AuthRoutes.REGISTER) { launchSingleTop = true } },
            )
        }
        composable(AuthRoutes.REGISTER) {
            RegisterRoute(
                onBack = { navController.popBackStack(AuthRoutes.LOGIN, inclusive = false) },
                onRegistered = onAuthSuccess,
            )
        }
        composable(AuthRoutes.FORGOT_PASSWORD) {
            ForgotPasswordRoute(
                onBack = { navController.popBackStack(AuthRoutes.LOGIN, inclusive = false) },
            )
        }
    }
}

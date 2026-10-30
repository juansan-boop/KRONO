package com.krono.feature.profile.domain

/**
 * Errores conocidos de las operaciones de perfil, preferencias y Zona de Seguridad (F-15, F-19, F-20, F-21).
 *
 * Permiten a la capa de presentación distinguir la causa sin depender de Firebase ni del
 * texto del mensaje, para mostrar copy localizado en `strings.xml`.
 */
sealed class ProfileOperationException(message: String) : Exception(message)

/** No hay conexión a internet: la operación no se inició y no se modificó ningún dato. */
class NetworkUnavailableException : ProfileOperationException("Sin conexión a internet")

/** El servidor no confirmó la operación dentro del tiempo límite. */
class RemoteTimeoutException : ProfileOperationException("La operación remota superó el tiempo límite")

/** La contraseña de reautenticación es incorrecta. */
class InvalidPasswordException : ProfileOperationException("Contraseña incorrecta")

/**
 * Datos de entrada inválidos en una operación de perfil.
 *
 * Su mensaje es solo diagnóstico: la UI nunca lo muestra, sino que lo traduce a copy en `strings.xml`.
 */
sealed class ProfileValidationException(message: String) : ProfileOperationException(message)

/** El nombre del perfil está vacío o en blanco. */
class BlankNameException : ProfileValidationException("Blank profile name")

/** El rol del perfil está vacío o en blanco. */
class BlankRoleException : ProfileValidationException("Blank profile role")

/** Los minutos de anticipación del aviso de entrega no son mayores que cero. */
class InvalidReminderMinutesException : ProfileValidationException("Reminder minutes must be positive")

/** La contraseña de reautenticación está vacía o en blanco. */
class BlankPasswordException : ProfileValidationException("Blank reauthentication password")

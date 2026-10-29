package com.krono.feature.profile.domain

/**
 * Errores conocidos de las operaciones destructivas de la Zona de Seguridad (F-21).
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

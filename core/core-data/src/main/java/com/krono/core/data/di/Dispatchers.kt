package com.krono.core.data.di

import javax.inject.Qualifier

/** Dispatcher para trabajo de disco y criptografía (Room, PBKDF2). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

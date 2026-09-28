package com.stacknoise.haac.core.network.di

import javax.inject.Qualifier

/** The app-wide coroutine scope of the live connection; runs on the main thread (concept 11.4). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConnectionScope

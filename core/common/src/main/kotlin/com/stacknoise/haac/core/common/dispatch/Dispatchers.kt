package com.stacknoise.haac.core.common.dispatch

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The dispatcher for blocking I/O (files, sockets, Keystore); the only place that names it is [DispatcherModule]. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** The dispatcher for CPU work such as JSON decoding and diffing. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** The main thread; runs a call made on the main thread at once. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/** Provides the coroutine dispatchers (concept 17.2), so tests can replace them. */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    /** [Dispatchers.IO]. */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** [Dispatchers.Default]. */
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /** [Dispatchers.Main] with `immediate`. */
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}

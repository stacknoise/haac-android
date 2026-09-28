package com.stacknoise.haac.core.error.di

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Provides the [ErrorFactory] implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {
    /** Binds [DefaultErrorFactory] as the app-wide [ErrorFactory]. */
    @Binds
    abstract fun bindErrorFactory(factory: DefaultErrorFactory): ErrorFactory
}

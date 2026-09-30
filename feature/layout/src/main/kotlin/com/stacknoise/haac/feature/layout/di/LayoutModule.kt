package com.stacknoise.haac.feature.layout.di

import com.stacknoise.haac.feature.layout.data.AreaSource
import com.stacknoise.haac.feature.layout.data.BridgeAreaSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the layout topic. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LayoutModule {
    /** The Home Assistant areas come from the bridge (concept 6.3). */
    @Binds
    abstract fun bindAreaSource(source: BridgeAreaSource): AreaSource
}

package com.stacknoise.haac.core.network.di

import com.stacknoise.haac.core.network.demo.DemoWorldStore
import com.stacknoise.haac.core.network.demo.FileDemoWorldStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Bindings of the demo instance (concept 20). */
@Module
@InstallIn(SingletonComponent::class)
abstract class DemoBindings {
    /** The saved state of the demo bridge (concept 20.5). */
    @Binds
    abstract fun bindDemoWorldStore(store: FileDemoWorldStore): DemoWorldStore
}

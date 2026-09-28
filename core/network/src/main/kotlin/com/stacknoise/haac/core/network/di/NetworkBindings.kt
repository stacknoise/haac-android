package com.stacknoise.haac.core.network.di

import com.stacknoise.haac.core.network.bridge.BridgeMessageFactory
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.session.DefaultInstanceSessionFactory
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Factory bindings of the network topic (concept 17.2). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    /** WebSocket messages with increasing ids. */
    @Binds
    abstract fun bindBridgeMessageFactory(factory: DefaultBridgeMessageFactory): BridgeMessageFactory

    /** Per-instance sessions. */
    @Binds
    abstract fun bindInstanceSessionFactory(factory: DefaultInstanceSessionFactory): InstanceSessionFactory
}

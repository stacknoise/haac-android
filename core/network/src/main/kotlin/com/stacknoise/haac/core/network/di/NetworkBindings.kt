package com.stacknoise.haac.core.network.di

import com.stacknoise.haac.core.network.bridge.BridgeMessageFactory
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.connection.Backoff
import com.stacknoise.haac.core.network.connection.BridgeConnector
import com.stacknoise.haac.core.network.connection.ConnectionSupervisor
import com.stacknoise.haac.core.network.connection.ConnectivityNetworkMonitor
import com.stacknoise.haac.core.network.connection.DefaultBridgeConnector
import com.stacknoise.haac.core.network.connection.ExponentialBackoff
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.connection.NetworkMonitor
import com.stacknoise.haac.core.network.discovery.NsdServerDiscovery
import com.stacknoise.haac.core.network.discovery.ServerDiscovery
import com.stacknoise.haac.core.network.endpoint.AuthProvidersProbe
import com.stacknoise.haac.core.network.endpoint.DefaultEndpointSelector
import com.stacknoise.haac.core.network.endpoint.EndpointProbe
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.HomeNetworkCheck
import com.stacknoise.haac.core.network.endpoint.NsdHomeNetworkCheck
import com.stacknoise.haac.core.network.session.DefaultInstanceSessionFactory
import com.stacknoise.haac.core.network.session.InstanceSessionFactory
import com.stacknoise.haac.core.network.websocket.HaWebSocketFactory
import com.stacknoise.haac.core.network.websocket.OkHttpWebSocketFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Factory and service bindings of the network topic (concept 17.2). */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindings {
    /** WebSocket messages with increasing ids. */
    @Binds
    abstract fun bindBridgeMessageFactory(factory: DefaultBridgeMessageFactory): BridgeMessageFactory

    /** Per-instance sessions. */
    @Binds
    abstract fun bindInstanceSessionFactory(factory: DefaultInstanceSessionFactory): InstanceSessionFactory

    /** LAN discovery via NsdManager (concept 4.2). */
    @Binds
    abstract fun bindServerDiscovery(discovery: NsdServerDiscovery): ServerDiscovery

    /** Home network check via mDNS (concept 4.5). */
    @Binds
    abstract fun bindHomeNetworkCheck(check: NsdHomeNetworkCheck): HomeNetworkCheck

    /** Reachability probe before a token is sent (concept 4.5). */
    @Binds
    abstract fun bindEndpointProbe(probe: AuthProvidersProbe): EndpointProbe

    /** Address selection (concept 4.5). */
    @Binds
    abstract fun bindEndpointSelector(selector: DefaultEndpointSelector): EndpointSelector

    /** WebSockets to HA via OkHttp. */
    @Binds
    abstract fun bindHaWebSocketFactory(factory: OkHttpWebSocketFactory): HaWebSocketFactory

    /** Handshake of the live connection (concept 9.1). */
    @Binds
    abstract fun bindBridgeConnector(connector: DefaultBridgeConnector): BridgeConnector

    /** Default network changes (concept 4.5). */
    @Binds
    abstract fun bindNetworkMonitor(monitor: ConnectivityNetworkMonitor): NetworkMonitor

    /** Reconnect back-off (concept 11.4). */
    @Binds
    abstract fun bindBackoff(backoff: ExponentialBackoff): Backoff

    /** The live connection for service calls (concept 8.1). */
    @Binds
    abstract fun bindLiveConnection(supervisor: ConnectionSupervisor): LiveConnection
}

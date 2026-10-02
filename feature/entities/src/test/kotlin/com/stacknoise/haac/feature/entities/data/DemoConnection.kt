package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.connection.BridgeConnection
import com.stacknoise.haac.core.network.connection.connectDemo
import com.stacknoise.haac.core.network.demo.DemoWorld
import com.stacknoise.haac.core.network.demo.MemoryDemoWorldStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.serialization.json.Json

/** The demo world in memory; a second connection over the same world sees what the first one changed. */
internal fun demoWorld() = DemoWorld(MemoryDemoWorldStore(), Json { ignoreUnknownKeys = true })

/** A real [BridgeConnection] over the built-in demo bridge (concept 20.7); replies come at once. */
internal fun TestScope.demoConnection(world: DemoWorld = demoWorld()): BridgeConnection =
    connectDemo(
        world, DefaultBridgeMessageFactory(), DefaultErrorFactory(), backgroundScope,
        dispatcher = Dispatchers.Unconfined,
    )

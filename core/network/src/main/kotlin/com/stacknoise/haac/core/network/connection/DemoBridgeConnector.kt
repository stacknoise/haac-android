package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.network.bridge.BridgeMessageFactory
import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.core.network.demo.DemoSocket
import com.stacknoise.haac.core.network.demo.DemoWorld
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * [BridgeConnector] that routes the demo instance to the built-in bridge (concept 20.2, 20.6) and every other
 * instance to [real]. For the server ID `demo` nothing touches the network: no address selection, no session, no
 * handshake; the connection runs over a [DemoSocket]. Only the server ID decides, so the demo can never reach a
 * real server and no real instance can reach the demo.
 */
class DemoBridgeConnector internal constructor(
    private val real: BridgeConnector,
    private val world: DemoWorld,
    private val messages: BridgeMessageFactory,
    private val errors: ErrorFactory,
) : BridgeConnector {
    /** Routes to the default connector in the app. */
    @Inject
    constructor(
        real: DefaultBridgeConnector,
        world: DemoWorld,
        messages: BridgeMessageFactory,
        errors: ErrorFactory,
    ) : this(real as BridgeConnector, world, messages, errors)

    /** The fixed demo address for the demo; the selection rules of the real connector for every other instance. */
    override suspend fun select(serverId: String): HttpUrl =
        if (DemoInstance.isDemo(serverId)) DemoInstance.ADDRESS.toHttpUrl() else real.select(serverId)

    /** A connection over a [DemoSocket] for the demo, the handshake of the real connector for every other instance. */
    override suspend fun connect(serverId: String, url: HttpUrl, scope: CoroutineScope): BridgeConnection =
        if (DemoInstance.isDemo(serverId)) {
            connectDemo(world, messages, errors, scope, url)
        } else {
            real.connect(serverId, url, scope)
        }
}

/**
 * A [BridgeConnection] over a [DemoSocket] of [world] at [url], with no handshake and no network (concept 20.2). The
 * connector uses it; so do the tests of the consumers, with a [dispatcher] that answers at once.
 */
fun connectDemo(
    world: DemoWorld,
    messages: BridgeMessageFactory,
    errors: ErrorFactory,
    scope: CoroutineScope,
    url: HttpUrl = DemoInstance.ADDRESS.toHttpUrl(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
): BridgeConnection =
    BridgeConnection(url, DemoInstance.info, DemoSocket(world, dispatcher), messages, errors, scope)

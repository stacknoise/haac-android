package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.BridgeUrls

/** Constants of the built-in demo instance (concept 20.2); no real instance can have this server ID. */
object DemoInstance {
    /** The server ID of the demo; real instances have random UUIDs. */
    const val SERVER_ID = "demo"

    /** Name of the instance and of its HA user. */
    const val DISPLAY_NAME = "Demo"

    /** The fixed `instance_id` of the demo bridge. */
    const val INSTANCE_ID = "d3a0d3a0d3a0d3a0d3a0d3a0d3a0d3a0"

    /** Reserved top-level domain `.invalid` never resolves; the instance row only needs some address. */
    const val ADDRESS = "https://demo.haac.invalid/"

    /**
     * The placeholder token of the demo (concept 20.4): a constant, not a secret. It is stored through the normal
     * `TokenStore` only so that start routing, the switcher and the settings treat the demo like any instance; it is
     * never sent anywhere and gives access to nothing.
     */
    const val PLACEHOLDER_TOKEN = "demo-placeholder-token"

    /** The HA user id that owns the demo schedules. */
    const val USER_ID = "demo-user"

    /** The `haac_bridge/info` of the demo: API version 1 with the optional feature `schedules`. */
    val info = BridgeInfo(
        bridgeVersion = "demo",
        apiVersion = 1,
        domains = listOf("climate", "sensor", "switch"),
        haVersion = "demo",
        instanceId = INSTANCE_ID,
        urls = BridgeUrls(),
        features = listOf("schedules"),
    )

    /** True for the server ID of the demo. */
    fun isDemo(serverId: String): Boolean = serverId == SERVER_ID
}

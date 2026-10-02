package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.network.demo.DemoInstance
import com.stacknoise.haac.feature.onboarding.domain.DemoEntry
import javax.inject.Inject

/** [DemoEntry] over the [InstanceRegistry]: the demo is one stored instance like any other (concept 20.4). */
class RegistryDemoEntry @Inject constructor(private val registry: InstanceRegistry) : DemoEntry {
    /** True if the registry has no instance with the demo's server ID. */
    override suspend fun available(): Boolean = registry.find(DemoInstance.SERVER_ID) == null

    /** Stores the demo instance and makes it active. */
    override suspend fun start(): String = registry.saveDemo()
}

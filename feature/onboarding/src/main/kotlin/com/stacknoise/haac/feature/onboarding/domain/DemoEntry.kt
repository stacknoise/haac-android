package com.stacknoise.haac.feature.onboarding.domain

/** The way into the demo instance from M-01 (concept 20.4). */
interface DemoEntry {
    /** True while no demo instance exists, so *Try the demo* is shown. */
    suspend fun available(): Boolean

    /** Creates the demo instance with its placeholder token, makes it active and returns its id. */
    suspend fun start(): String
}

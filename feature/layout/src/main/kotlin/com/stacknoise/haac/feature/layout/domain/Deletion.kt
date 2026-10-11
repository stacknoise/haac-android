package com.stacknoise.haac.feature.layout.domain

/**
 * A deletion that can still be undone (concept 6.2): place [name] of [kind] of instance [serverId] and everything
 * deleted with it were marked at [at].
 */
data class Deletion(val kind: PlaceKind, val name: String, val at: Long, val serverId: String)

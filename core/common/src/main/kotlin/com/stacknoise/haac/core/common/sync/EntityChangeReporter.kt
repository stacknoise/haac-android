package com.stacknoise.haac.core.common.sync

/**
 * Receives the entity changes a sync found and turns them into entries of the notification list (concept 9.1,
 * M-09). Implemented by `:feature:notifications`, called by the sync in `:feature:entities`.
 */
fun interface EntityChangeReporter {
    /** Records the entities newly exposed to instance [serverId] ([added]) and those withdrawn ([removed]). */
    suspend fun report(serverId: String, added: List<String>, removed: List<String>)
}

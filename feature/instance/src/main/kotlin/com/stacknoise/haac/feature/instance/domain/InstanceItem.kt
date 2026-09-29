package com.stacknoise.haac.feature.instance.domain

import com.stacknoise.haac.core.database.server.ServerEntity
import java.net.URI

/**
 * One row of the instance switcher (concept 4.4): [name] with its accent colour and the [address] host in use.
 * [userName] is set only when the same HA server is stored for several HA users, so the rows can be told apart.
 */
data class InstanceItem(
    val id: String,
    val name: String,
    val userName: String?,
    val accent: Long,
    val address: String,
    val active: Boolean,
)

/** The switcher rows of these instances; [activeId] is marked (concept 4.4). */
fun List<ServerEntity>.toItems(activeId: String?): List<InstanceItem> {
    val shared = mapNotNull { it.instanceUuid }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    return map { server ->
        InstanceItem(
            id = server.id,
            name = server.displayName,
            userName = server.haUserName.takeIf { server.instanceUuid in shared },
            accent = server.accentColor,
            address = server.hostLabel(),
            active = server.id == activeId,
        )
    }
}

/** The host of the external address, else of the internal one (empty if the stored text is no address). */
private fun ServerEntity.hostLabel(): String {
    val url = externalUrl ?: internalUrl ?: return ""
    return runCatching { URI(url).host }.getOrNull().orEmpty()
}

package com.stacknoise.haac.core.network.discovery

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Builds a [DiscoveredServer] from a resolved `_home-assistant._tcp` service and its TXT record. */
object HaServiceParser {
    /** Zeroconf service type HA announces (concept 4.2). */
    const val SERVICE_TYPE = "_home-assistant._tcp"

    /** Returns the server; the URL is the TXT `base_url`, else `http://<ip>:<port>` as HA announces it. */
    fun parse(serviceName: String, ip: String, port: Int, txt: Map<String, ByteArray?>): DiscoveredServer {
        val attributes = txt.mapValues { (_, value) -> value?.decodeToString()?.takeIf { it.isNotBlank() } }
        val address = if (ip.contains(':')) "[$ip]:$port" else "$ip:$port"
        val txtHosts = listOf("base_url", "internal_url").mapNotNull { attributes[it]?.toHttpUrlOrNull()?.host }
        return DiscoveredServer(
            id = attributes["uuid"] ?: serviceName,
            name = attributes["location_name"] ?: serviceName,
            address = address,
            url = attributes["base_url"] ?: attributes["internal_url"] ?: "http://$address",
            version = attributes["version"],
            hosts = (txtHosts + ip).map(::normalizeHost).toSet(),
        )
    }

    /** Lower case without IPv6 brackets and zone, so hosts from URLs and NSD compare equal. */
    fun normalizeHost(host: String): String =
        host.removePrefix("[").removeSuffix("]").substringBefore('%').lowercase()
}

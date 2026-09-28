package com.stacknoise.haac.core.network.server

/**
 * Decides whether a host is in the user's own network (concept 4.3): RFC 1918, loopback and
 * link-local addresses, IPv6 unique-local/link-local/loopback and `.local` mDNS names.
 *
 * Works on the literal host only; names are never resolved via DNS.
 */
@Suppress("MagicNumber") // address ranges are clearer as literal octets and prefixes
object PrivateAddress {
    private val ipv4 = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

    /** Returns true if [host] is a private address or a `.local` name. */
    fun isPrivate(host: String): Boolean {
        val normalized = host.trim().trimEnd('.').removePrefix("[").removeSuffix("]").lowercase()
        return when {
            normalized == "localhost" || normalized.endsWith(".local") -> true
            ipv4.matches(normalized) -> isPrivateIpv4(normalized)
            normalized.contains(':') -> isPrivateIpv6(normalized)
            else -> false
        }
    }

    /** RFC 1918, loopback 127/8 and link-local 169.254/16. */
    private fun isPrivateIpv4(host: String): Boolean {
        val octets = ipv4.matchEntire(host)!!.groupValues.drop(1).map { it.toInt() }
        if (octets.any { it > 255 }) return false
        val (a, b) = octets
        return a == 10 || a == 127 || (a == 172 && b in 16..31) || (a == 192 && b == 168) || (a == 169 && b == 254)
    }

    /** Loopback ::1, unique-local fc00::/7 and link-local fe80::/10. */
    private fun isPrivateIpv6(host: String): Boolean {
        val address = host.substringBefore('%')
        val first = address.substringBefore(':').takeIf { it.isNotEmpty() }?.toIntOrNull(16)
        return address == "::1" ||
            (first != null && (first and 0xFE00 == 0xFC00 || first and 0xFFC0 == 0xFE80))
    }
}

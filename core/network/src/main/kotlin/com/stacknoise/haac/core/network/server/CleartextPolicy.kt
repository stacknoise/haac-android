package com.stacknoise.haac.core.network.server

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import okhttp3.HttpUrl

/**
 * HTTPS by default; plain `http://` only to private addresses (concept 4.3).
 *
 * The network security config permits cleartext because Android cannot express address ranges
 * there, so this check is the one that enforces the rule before any request leaves the app.
 */
object CleartextPolicy {
    /** Throws HAAC-NET-006 if [url] is `http://` to a host outside the private ranges. */
    fun requireAllowed(url: HttpUrl) {
        if (!url.isHttps && !PrivateAddress.isPrivate(url.host)) {
            throw NetworkException(ErrorCode.NET_CLEARTEXT_NOT_ALLOWED)
        }
    }

    /** Returns true if [url] is unencrypted and the user must confirm the warning first. */
    fun needsWarning(url: HttpUrl): Boolean = !url.isHttps
}

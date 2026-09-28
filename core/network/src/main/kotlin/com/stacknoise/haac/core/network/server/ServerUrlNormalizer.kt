package com.stacknoise.haac.core.network.server

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Turns user input into the base URL of a HA server (concept 4.2, step 2). */
object ServerUrlNormalizer {
    private val scheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /** Adds https, removes paths, query and trailing slashes; throws HAAC-NET-005 if nothing usable is left. */
    fun normalize(input: String): HttpUrl {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) throw invalid()
        val withScheme = if (scheme.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val url = withScheme.toHttpUrlOrNull() ?: throw invalid()
        return url.newBuilder().encodedPath("/").query(null).fragment(null).username("").password("").build()
    }

    /** The error for input that is not a usable http(s) address. */
    private fun invalid() = NetworkException(ErrorCode.NET_INVALID_ADDRESS)
}

package com.stacknoise.haac.core.network.auth

/**
 * OAuth identity of the app towards HA (concept 5.1, 16.8). Never change these URLs:
 * existing refresh tokens are bound to the client_id.
 */
object AuthClientConfig {
    /** Shown by HA as the app's identity. */
    const val CLIENT_ID = "https://stacknoise.com/haac/"

    /** Same host as [CLIENT_ID], so HA accepts it without fetching anything. */
    const val REDIRECT_URI = "https://stacknoise.com/haac/auth-callback"
}

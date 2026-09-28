package com.stacknoise.haac.core.security.token

/** Keeps the HA refresh token of each instance encrypted at rest (concept 5.2, 5.3). */
interface TokenStore {
    /** Encrypts [refreshToken] with the device-bound key of [serverId] and replaces an older token and its keys. */
    suspend fun save(serverId: String, refreshToken: String)

    /**
     * Returns the refresh token of [serverId], or null if none is stored. A fingerprint-protected token is
     * returned only while it is unlocked, otherwise HAAC-SEC-003 (concept 5.4).
     */
    suspend fun read(serverId: String): String?

    /** Returns true if a refresh token is stored for [serverId]; does not decrypt it. */
    suspend fun contains(serverId: String): Boolean

    /** Returns how the token of [serverId] is protected, or null if none is stored; does not decrypt it. */
    suspend fun protection(serverId: String): TokenProtection?

    /** Deletes the ciphertext, the unlocked copy and every Keystore key of [serverId]. */
    suspend fun delete(serverId: String)
}

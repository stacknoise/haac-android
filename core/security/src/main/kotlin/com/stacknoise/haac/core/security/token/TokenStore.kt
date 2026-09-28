package com.stacknoise.haac.core.security.token

/** Keeps the HA refresh token of each instance encrypted at rest (concept 5.2, 5.3). */
interface TokenStore {
    /** Encrypts and stores [refreshToken] for [serverId], replacing an older one. */
    suspend fun save(serverId: String, refreshToken: String)

    /** Returns the decrypted refresh token of [serverId], or null if none is stored. */
    suspend fun read(serverId: String): String?

    /** Returns true if a refresh token is stored for [serverId]; does not decrypt it. */
    suspend fun contains(serverId: String): Boolean

    /** Deletes the ciphertext and the Keystore key of [serverId]. */
    suspend fun delete(serverId: String)
}

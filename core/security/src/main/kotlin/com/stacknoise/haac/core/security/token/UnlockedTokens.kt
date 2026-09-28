package com.stacknoise.haac.core.security.token

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refresh tokens of fingerprint-protected instances after a successful fingerprint (concept 5.4). They live
 * in memory only, until the app locks (5.5), the user signs out or the token is replaced.
 */
@Singleton
class UnlockedTokens @Inject constructor() {
    private val tokens = ConcurrentHashMap<String, String>()

    /** The unlocked token of [serverId], or null while it is locked. */
    fun get(serverId: String): String? = tokens[serverId]

    /** Keeps [refreshToken] of [serverId] until the next lock. */
    fun put(serverId: String, refreshToken: String) {
        tokens[serverId] = refreshToken
    }

    /** Forgets the token of [serverId]. */
    fun remove(serverId: String) {
        tokens.remove(serverId)
    }

    /** Forgets all tokens: the app is locked again. */
    fun clear() = tokens.clear()
}

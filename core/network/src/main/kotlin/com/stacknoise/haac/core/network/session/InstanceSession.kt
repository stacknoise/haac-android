package com.stacknoise.haac.core.network.session

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.auth.AccessToken
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.security.token.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl

/** Creates the session of one instance (concept 4.4, 17.2). */
interface InstanceSessionFactory {
    /**
     * A session for the instance [serverId] at [baseUrl]; nothing is shared with other instances. [verified] is
     * false for an address that is not stored and confirmed yet; a refresh token it rejects is kept.
     */
    fun create(serverId: String, baseUrl: HttpUrl, verified: Boolean = true): InstanceSession
}

/** Default [InstanceSessionFactory]. */
class DefaultInstanceSessionFactory @Inject constructor(
    private val tokens: TokenClient,
    private val store: TokenStore,
) : InstanceSessionFactory {
    /** Wires the token client and store into a new session. */
    override fun create(serverId: String, baseUrl: HttpUrl, verified: Boolean): InstanceSession =
        InstanceSession(serverId, baseUrl, tokens, store, verified)
}

/**
 * Token handling of one instance (concept 5.2): the access token lives only in memory and is refreshed
 * shortly before it expires; a refresh token HA rejects is deleted, so the app returns to the login. Only a
 * [verified] address may force that: any server can answer 400 at an address nobody confirmed (review S-12).
 */
class InstanceSession(
    val serverId: String,
    val baseUrl: HttpUrl,
    private val tokens: TokenClient,
    private val store: TokenStore,
    private val verified: Boolean = true,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cached: AccessToken? = null
    private var expiresAt = 0L

    /** A valid access token; HAAC-AUTH-003 if the refresh token is missing or revoked (HAAC-NET-001 if unverified). */
    suspend fun accessToken(): String = mutex.withLock {
        cached?.takeIf { clock() < expiresAt - REFRESH_MARGIN_MS }?.let { return it.token }
        val refreshToken = store.read(serverId) ?: throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
        val fresh = try {
            tokens.refresh(baseUrl, refreshToken)
        } catch (e: AuthException) {
            if (e.code != ErrorCode.AUTH_SESSION_EXPIRED) throw e
            if (!verified) throw NetworkException(ErrorCode.NET_UNREACHABLE, e)
            store.delete(serverId)
            throw e
        }
        cached = fresh
        expiresAt = clock() + fresh.expiresInSeconds * MS_PER_SECOND
        fresh.token
    }

    /** Drops the access token, e.g. after `auth_invalid` on the WebSocket; the next call refreshes. */
    suspend fun invalidate() = mutex.withLock { cached = null }

    /**
     * Logout (concept 5.2): revokes the refresh token in HA, then deletes ciphertext and key.
     * Returns false if HA could not be reached or the fingerprint-protected token is locked; the local token
     * is deleted anyway.
     */
    suspend fun signOut(): Boolean = mutex.withLock {
        cached = null
        val revoked = try {
            store.read(serverId)?.let { tokens.revoke(baseUrl, it) }
            true
        } catch (_: HaacException) {
            false
        } finally {
            store.delete(serverId)
        }
        revoked
    }

    /** Refresh timing. */
    private companion object {
        const val REFRESH_MARGIN_MS = 60_000L
        const val MS_PER_SECOND = 1_000L
    }
}

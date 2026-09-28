package com.stacknoise.haac.core.network.session

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.auth.AccessToken
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.security.token.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl

/** Creates the session of one instance (concept 4.4, 17.2). */
interface InstanceSessionFactory {
    /** A session for the instance [serverId] at [baseUrl]; nothing is shared with other instances. */
    fun create(serverId: String, baseUrl: HttpUrl): InstanceSession
}

/** Default [InstanceSessionFactory]. */
class DefaultInstanceSessionFactory @Inject constructor(
    private val tokens: TokenClient,
    private val store: TokenStore,
) : InstanceSessionFactory {
    /** Wires the token client and store into a new session. */
    override fun create(serverId: String, baseUrl: HttpUrl): InstanceSession =
        InstanceSession(serverId, baseUrl, tokens, store)
}

/**
 * Token handling of one instance (concept 5.2): the access token lives only in memory and is refreshed
 * shortly before it expires; a refresh token HA rejects is deleted, so the app returns to the login.
 */
class InstanceSession(
    val serverId: String,
    val baseUrl: HttpUrl,
    private val tokens: TokenClient,
    private val store: TokenStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()
    private var cached: AccessToken? = null
    private var expiresAt = 0L

    /** A valid access token; HAAC-AUTH-003 if there is no refresh token or HA revoked it. */
    suspend fun accessToken(): String = mutex.withLock {
        cached?.takeIf { clock() < expiresAt - REFRESH_MARGIN_MS }?.let { return it.token }
        val refreshToken = store.read(serverId) ?: throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
        val fresh = try {
            tokens.refresh(baseUrl, refreshToken)
        } catch (e: AuthException) {
            if (e.code == ErrorCode.AUTH_SESSION_EXPIRED) store.delete(serverId)
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
     * Returns false if HA could not be reached; the local token is deleted anyway.
     */
    suspend fun signOut(): Boolean = mutex.withLock {
        cached = null
        val refreshToken = store.read(serverId)
        val revoked = try {
            refreshToken?.let { tokens.revoke(baseUrl, it) }
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

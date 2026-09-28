package com.stacknoise.haac.feature.onboarding.domain

import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import okhttp3.HttpUrl

/** Where HA's login flow stands after credentials or a code were sent (concept 5.1). */
sealed interface SignInStep {
    /** HA asks for the MFA code of the flow [flowId]. */
    data class CodeRequired(val flowId: String) : SignInStep

    /** HA accepted the login; [tokens] stay in memory until the instance is saved. */
    class Authorized(val tokens: AuthTokens) : SignInStep {
        /** Hides the tokens. */
        override fun toString() = "Authorized(***)"
    }
}

/** Outcome of the bridge check after a sign-in (concept 4.2 steps 4–6). */
sealed interface SignInResult {
    /** The instance [serverId] was stored and is active. */
    data class Saved(val serverId: String) : SignInResult

    /**
     * The server and HA user already belong to the stored instance [serverId] (concept 4.5): the address
     * can be added in [slot], replacing [replaces] if that slot is set. Nothing was stored yet.
     */
    data class SameInstance(
        val serverId: String,
        val displayName: String,
        val slot: AddressSlot,
        val replaces: String?,
    ) : SignInResult
}

/** The instance a sign-in is for: a new one, or the stored instance [serverId] that lost its token (concept 4.1). */
data class SignInTarget(val url: HttpUrl, val displayName: String, val serverId: String? = null)

/** A stored instance shown when only the login is missing (concept 4.1); [url] is the address chosen by 4.5. */
data class KnownServer(val id: String, val url: String, val displayName: String, val userName: String)

/** Login against HA and creation of the instance (concept 4.2 step 4, 4.4, 4.5, 5.1–5.3). */
interface SignInRepository {
    /** The stored instance [serverId] with its current address, or null if it no longer exists. */
    suspend fun knownServer(serverId: String): KnownServer?

    /** Starts the login flow and sends the credentials; [password] is not kept and not changed. */
    suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep

    /** Sends the MFA [code] of the flow [flowId]. */
    suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep

    /**
     * Checks HAAC Bridge with the new access token, then stores the encrypted refresh token and the
     * instance and makes it active; BRG-001/002 if the bridge is missing or too old, NET-008 if a stored
     * instance signs in to a different server. Returns [SignInResult.SameInstance] instead of storing a
     * duplicate (concept 4.5).
     */
    suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): SignInResult

    /**
     * Adds [url] to the stored instance [serverId] and makes it active (concept 4.5). [tokens] of the new
     * sign-in replace a missing token of the instance; otherwise they are revoked.
     */
    suspend fun addAddress(serverId: String, url: HttpUrl, tokens: AuthTokens)

    /** Revokes tokens that will not be used; false if HA could not be reached (they then expire in HA). */
    suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean
}

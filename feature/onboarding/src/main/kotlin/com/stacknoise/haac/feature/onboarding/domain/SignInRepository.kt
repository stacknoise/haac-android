package com.stacknoise.haac.feature.onboarding.domain

import com.stacknoise.haac.core.network.auth.AuthTokens
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

/** The instance a sign-in is for: a new one, or the stored instance [serverId] that lost its token (concept 4.1). */
data class SignInTarget(val url: HttpUrl, val displayName: String, val serverId: String? = null)

/** A stored instance shown when only the login is missing (concept 4.1). */
data class KnownServer(val id: String, val url: String, val displayName: String, val userName: String)

/** Login against HA and creation of the instance (concept 4.2 step 4, 4.4, 5.1–5.3). */
interface SignInRepository {
    /** The stored instance [serverId], or null if it no longer exists. */
    suspend fun knownServer(serverId: String): KnownServer?

    /** Starts the login flow and sends the credentials; [password] is not kept and not changed. */
    suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep

    /** Sends the MFA [code] of the flow [flowId]. */
    suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep

    /**
     * Checks HAAC Bridge with the new access token, then stores the encrypted refresh token and the
     * instance and makes it active. Returns the instance id; BRG-001/002 if the bridge is missing or too old.
     */
    suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): String

    /** Revokes tokens that will not be used; false if HA could not be reached (they then expire in HA). */
    suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean
}

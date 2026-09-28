package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.http.HaHttpClient
import com.stacknoise.haac.core.network.http.HaResponse
import com.stacknoise.haac.core.network.http.decodeOrUnexpected
import com.stacknoise.haac.core.network.http.endpoint
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import javax.inject.Inject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.HttpUrl

/** Tokens issued after login (concept 5.2); [toString] hides them so they never reach a log. */
class AuthTokens(val accessToken: String, val refreshToken: String, val expiresInSeconds: Long) {
    /** Hides the tokens. */
    override fun toString() = "AuthTokens(***)"
}

/** A new access token from a refresh; memory only (concept 5.2). */
class AccessToken(val token: String, val expiresInSeconds: Long) {
    /** Hides the token. */
    override fun toString() = "AccessToken(***)"
}

/** Answer of `POST /auth/token`. */
@Serializable
private class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
)

/** `POST /auth/token` and `POST /auth/revoke` (concept 5.1, 5.2, 11.1). */
class TokenClient @Inject constructor(
    private val http: HaHttpClient,
    private val json: Json,
) {
    /** Exchanges the login flow's authorization code; an expired code is HAAC-AUTH-005. */
    suspend fun exchangeCode(baseUrl: HttpUrl, code: String): AuthTokens {
        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("client_id", AuthClientConfig.CLIENT_ID)
            .build()
        val response = http.post(baseUrl.endpoint(TOKEN_PATH), form)
        val tokens = parse(response, rejected = ErrorCode.AUTH_SIGN_IN_ABORTED)
        return AuthTokens(tokens.accessToken, tokens.refreshToken ?: throw UnexpectedException(), tokens.expiresIn)
    }

    /** Gets a new access token; a revoked or expired refresh token is HAAC-AUTH-003 (concept 5.2). */
    suspend fun refresh(baseUrl: HttpUrl, refreshToken: String): AccessToken {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", AuthClientConfig.CLIENT_ID)
            .build()
        val tokens = parse(http.post(baseUrl.endpoint(TOKEN_PATH), form), rejected = ErrorCode.AUTH_SESSION_EXPIRED)
        return AccessToken(tokens.accessToken, tokens.expiresIn)
    }

    /** Revokes [refreshToken] in HA; HA answers 200 even for unknown tokens (RFC 7009). */
    suspend fun revoke(baseUrl: HttpUrl, refreshToken: String) {
        val form = FormBody.Builder().add("token", refreshToken).build()
        val response = http.post(baseUrl.endpoint(REVOKE_PATH), form)
        if (!response.isSuccessful) throw UnexpectedException()
    }

    /** Reads the token JSON after checking the status code. */
    private fun parse(response: HaResponse, rejected: ErrorCode): TokenResponse {
        failure(response, rejected)?.let { throw it }
        return json.decodeOrUnexpected(TokenResponse.serializer(), response.body)
    }

    /** 400/401 become [rejected], 403 HAAC-AUTH-006, other errors HAAC-APP-000; null for success. */
    private fun failure(response: HaResponse, rejected: ErrorCode): HaacException? = when {
        response.code == HTTP_BAD_REQUEST || response.code == HTTP_UNAUTHORIZED -> AuthException(rejected)
        response.code == HTTP_FORBIDDEN -> AuthException(ErrorCode.AUTH_USER_BLOCKED)
        !response.isSuccessful -> UnexpectedException()
        else -> null
    }

    /** Endpoints of the HA auth API. */
    private companion object {
        const val TOKEN_PATH = "auth/token"
        const val REVOKE_PATH = "auth/revoke"
    }
}

package com.stacknoise.haac.core.network.auth

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.HaHttpClient
import com.stacknoise.haac.core.network.http.endpoint
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.HttpUrl

/** One login provider of a HA server, e.g. type `homeassistant` for username and password. */
@Serializable
data class AuthProvider(
    val name: String,
    val type: String,
    val id: String? = null,
)

/** Reads `GET /auth/providers`, which confirms a HA server and lists its login providers (concept 4.2, 11.1). */
class AuthProvidersClient @Inject constructor(
    private val http: HaHttpClient,
    private val json: Json,
) {
    /** Returns the providers of the server at [baseUrl]; errors come as HaacException (NET-00x). */
    suspend fun fetch(baseUrl: HttpUrl): List<AuthProvider> {
        val response = http.get(baseUrl.endpoint("auth/providers"))
        if (!response.isSuccessful) throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
        return parse(response.body)
    }

    /** Accepts both the current object form `{"providers": [...]}` and the older plain list. */
    private fun parse(body: String): List<AuthProvider> = try {
        val list: JsonElement = when (val root = json.parseToJsonElement(body)) {
            is JsonObject -> root["providers"] ?: throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
            is JsonArray -> root
            else -> throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
        }
        json.decodeFromJsonElement<List<AuthProvider>>(list)
    } catch (e: SerializationException) {
        throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT, e)
    } catch (e: IllegalArgumentException) {
        throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT, e)
    }

    /** Provider types the app knows (concept 5.1). */
    companion object {
        /** Username and password login handled natively by the app. */
        const val PASSWORD_PROVIDER = "homeassistant"
    }
}

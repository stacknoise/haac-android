package com.stacknoise.haac.core.network.http

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.server.CleartextPolicy
import java.io.IOException
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.coroutines.executeAsync

/** Status code and body text of a HA response. */
class HaResponse(val code: Int, val body: String) {
    /** True for 2xx. */
    val isSuccessful: Boolean get() = code in SUCCESS

    /** Hides the body, which may contain tokens. */
    override fun toString() = "HaResponse($code)"

    /** Range of successful status codes. */
    private companion object {
        val SUCCESS = 200..299
    }
}

/**
 * Runs HTTP requests against a HA server: the cleartext rule (concept 4.3) and the error mapping (17.3)
 * are applied here once for every REST call. No logging, so auth request bodies never reach a log (5.1).
 */
class HaHttpClient @Inject constructor(
    private val client: OkHttpClient,
    private val errors: ErrorFactory,
) {
    /** Sends a GET request. */
    suspend fun get(url: HttpUrl): HaResponse = execute(Request.Builder().url(url).build())

    /** Sends a POST request with [body]. */
    suspend fun post(url: HttpUrl, body: RequestBody): HaResponse =
        execute(Request.Builder().url(url).post(body).build())

    /** Checks the cleartext rule, runs the call and converts I/O errors (NET-001, NET-007). */
    private suspend fun execute(request: Request): HaResponse {
        CleartextPolicy.requireAllowed(request.url)
        return try {
            client.newCall(request).executeAsync().use { HaResponse(it.code, it.body.boundedString()) }
        } catch (e: IOException) {
            throw errors.from(e)
        }
    }
}

/** The body as text; HAAC-NET-004 if it is larger than [MAX_BODY_BYTES], so a wrong server cannot fill the memory. */
private fun ResponseBody.boundedString(): String {
    val source = source()
    if (!source.request(MAX_BODY_BYTES + 1)) return string()
    throw NetworkException(ErrorCode.NET_NOT_HOME_ASSISTANT)
}

/** Largest accepted response body of a REST call. */
private const val MAX_BODY_BYTES = 1_000_000L

/** [this] base URL with [path] appended, e.g. `auth/token`. */
fun HttpUrl.endpoint(path: String): HttpUrl = newBuilder().addPathSegments(path).build()

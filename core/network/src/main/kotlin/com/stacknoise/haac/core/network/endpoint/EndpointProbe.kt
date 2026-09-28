package com.stacknoise.haac.core.network.endpoint

import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.network.auth.AuthProvidersClient
import com.stacknoise.haac.core.network.http.HaHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/** Checks that an address answers as HA before any token is sent there (concept 4.5). */
fun interface EndpointProbe {
    /** Returns normally if HA answers at [url]; otherwise throws the HaacException of the attempt. */
    suspend fun check(url: HttpUrl)
}

/** [EndpointProbe] with `GET /auth/providers` and a 3 s call timeout. */
class AuthProvidersProbe @Inject constructor(client: OkHttpClient, errors: ErrorFactory, json: Json) : EndpointProbe {
    private val providers = AuthProvidersClient(
        HaHttpClient(client.newBuilder().callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).build(), errors),
        json,
    )

    /** NET-001 if unreachable, NET-004 if something else answers, NET-006 for cleartext to a public host. */
    override suspend fun check(url: HttpUrl) {
        providers.fetch(url)
    }

    /** Probe timeout. */
    private companion object {
        const val TIMEOUT_SECONDS = 3L
    }
}

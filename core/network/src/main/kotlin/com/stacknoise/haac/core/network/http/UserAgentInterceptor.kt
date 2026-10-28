package com.stacknoise.haac.core.network.http

import okhttp3.Interceptor
import okhttp3.Response

/** Sets `User-Agent: HAAC/<version> (Android <sdk>)`, so HA's log shows which client sent a request. */
class UserAgentInterceptor(appVersion: String, sdkInt: Int) : Interceptor {
    private val value = "HAAC/$appVersion (Android $sdkInt)"

    /** Replaces the OkHttp default user agent of every request. */
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", value).build())
}

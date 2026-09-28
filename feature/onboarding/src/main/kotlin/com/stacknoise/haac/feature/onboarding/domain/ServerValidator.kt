package com.stacknoise.haac.feature.onboarding.domain

import okhttp3.HttpUrl

/** A reachable HA server that offers username and password login. */
data class ValidatedServer(val url: HttpUrl)

/** Confirms that a URL points to a HA server the app can sign in to (concept 4.2, step 3). */
interface ServerValidator {
    /** Returns the validated server or throws a HaacException (NET-00x, AUTH-004). */
    suspend fun validate(url: HttpUrl): ValidatedServer
}

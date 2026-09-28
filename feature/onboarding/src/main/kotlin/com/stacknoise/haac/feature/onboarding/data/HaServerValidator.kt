package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.network.auth.AuthProvidersClient
import com.stacknoise.haac.feature.onboarding.domain.ServerValidator
import com.stacknoise.haac.feature.onboarding.domain.ValidatedServer
import javax.inject.Inject
import okhttp3.HttpUrl

/** Validates a server via `GET /auth/providers` and requires the username/password provider (concept 4.2, 5.1). */
class HaServerValidator @Inject constructor(
    private val providers: AuthProvidersClient,
) : ServerValidator {
    /** Throws HAAC-AUTH-004 if the server offers no username/password login. */
    override suspend fun validate(url: HttpUrl): ValidatedServer {
        val available = providers.fetch(url)
        if (available.none { it.type == AuthProvidersClient.PASSWORD_PROVIDER }) {
            throw AuthException(ErrorCode.AUTH_PASSWORD_LOGIN_UNAVAILABLE)
        }
        return ValidatedServer(url)
    }
}

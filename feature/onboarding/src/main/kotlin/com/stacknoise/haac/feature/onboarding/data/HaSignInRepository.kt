package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.database.server.ServerEntity
import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.auth.LoginFlowClient
import com.stacknoise.haac.core.network.auth.LoginFlowStep
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.core.network.endpoint.AddressSlot
import com.stacknoise.haac.core.network.endpoint.EndpointSelector
import com.stacknoise.haac.core.network.endpoint.addresses
import com.stacknoise.haac.core.network.endpoint.requireSameInstance
import com.stacknoise.haac.feature.onboarding.domain.KnownServer
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import com.stacknoise.haac.feature.onboarding.domain.SignInResult
import com.stacknoise.haac.feature.onboarding.domain.SignInStep
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import javax.inject.Inject
import okhttp3.HttpUrl

/** Native login via HA's login flow API, bridge check and storage of the instance (concept 4.2, 4.5, 5.1). */
class HaSignInRepository @Inject constructor(
    private val flows: LoginFlowClient,
    private val tokens: TokenClient,
    private val bridge: BridgeInfoClient,
    private val registry: InstanceRegistry,
    private val endpoints: EndpointSelector,
) : SignInRepository {
    /**
     * Reads the stored instance for the login of concept 4.1 with the address chosen by 4.5. If no address
     * answers, the external or an `https://` internal address is shown, never an unconfirmed `http://` one.
     */
    override suspend fun knownServer(serverId: String): KnownServer? = registry.find(serverId)?.let { server ->
        val url = try {
            endpoints.select(server)
        } catch (e: HaacException) {
            server.addresses.run { external ?: internal?.takeIf { it.isHttps } } ?: throw e
        }
        KnownServer(server.id, url.toString(), server.displayName, server.haUserName)
    }

    /** A new flow per attempt, so a wrong password never leaves a half-used flow behind. */
    override suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep {
        val flow = flows.start(url) as? LoginFlowStep.Form ?: throw UnexpectedException()
        return next(url, flows.submitCredentials(url, flow.flowId, username, password))
    }

    /** Sends the code; a wrong one keeps the flow, so the user can try again. */
    override suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep =
        next(url, flows.submitMfaCode(url, flowId, code))

    /**
     * Bridge check first: without HAAC Bridge the instance is not stored. A stored instance must answer
     * with its own instance ID; a new sign-in to a stored instance and user becomes an address offer.
     */
    override suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): SignInResult {
        val info = bridge.fetch(target.url, tokens.accessToken)
        val stored = target.serverId?.let { registry.find(it) }
        requireSameInstance(stored?.instanceUuid, info.instanceId)
        val same = if (target.serverId == null) registry.findSame(info.instanceId, username) else null
        return same?.let { offer(it, target.url) }
            ?: SignInResult.Saved(registry.save(target, username, info, tokens.refreshToken))
    }

    /** Stores the address; the new tokens are kept only if the instance had none. */
    override suspend fun addAddress(serverId: String, url: HttpUrl, tokens: AuthTokens) {
        if (!registry.addAddress(serverId, url, tokens.refreshToken)) discard(url, tokens)
    }

    /** Best effort: an unrevoked token expires in HA after a period of inactivity (concept 5.2). */
    override suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean = try {
        this.tokens.revoke(url, tokens.refreshToken)
        true
    } catch (_: HaacException) {
        false
    }

    /** The offer to add [url] to [server] in its slot (concept 4.5). */
    private fun offer(server: ServerEntity, url: HttpUrl): SignInResult.SameInstance {
        val slot = AddressSlot.of(url)
        val current = server.addresses[slot]?.takeIf { it != url }
        return SignInResult.SameInstance(server.id, server.displayName, slot, current?.toString())
    }

    /**
     * Interprets a flow step: the code is exchanged for tokens, `invalid_auth`/`invalid_code` become
     * AUTH-001/002, and with several MFA modules the first one is chosen (the app asks only for a code).
     */
    private suspend fun next(url: HttpUrl, step: LoginFlowStep): SignInStep = when (step) {
        is LoginFlowStep.Done -> SignInStep.Authorized(tokens.exchangeCode(url, step.code))
        is LoginFlowStep.Form -> when {
            step.error == INVALID_AUTH -> throw AuthException(ErrorCode.AUTH_INVALID_CREDENTIALS)
            step.error == INVALID_CODE -> throw AuthException(ErrorCode.AUTH_INVALID_MFA_CODE)
            step.stepId == STEP_SELECT_MFA -> {
                val module = step.mfaModules.firstOrNull() ?: throw UnexpectedException()
                next(url, flows.selectMfaModule(url, step.flowId, module))
            }
            step.stepId == STEP_MFA -> SignInStep.CodeRequired(step.flowId)
            else -> throw UnexpectedException()
        }
    }

    /** Step ids and error keys of HA's login flow. */
    private companion object {
        const val INVALID_AUTH = "invalid_auth"
        const val INVALID_CODE = "invalid_code"
        const val STEP_SELECT_MFA = "select_mfa_module"
        const val STEP_MFA = "mfa"
    }
}

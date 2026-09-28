package com.stacknoise.haac.feature.onboarding.data

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.UnexpectedException
import com.stacknoise.haac.core.network.auth.AuthTokens
import com.stacknoise.haac.core.network.auth.LoginFlowClient
import com.stacknoise.haac.core.network.auth.LoginFlowStep
import com.stacknoise.haac.core.network.auth.TokenClient
import com.stacknoise.haac.core.network.bridge.BridgeInfoClient
import com.stacknoise.haac.feature.onboarding.domain.KnownServer
import com.stacknoise.haac.feature.onboarding.domain.SignInRepository
import com.stacknoise.haac.feature.onboarding.domain.SignInStep
import com.stacknoise.haac.feature.onboarding.domain.SignInTarget
import javax.inject.Inject
import okhttp3.HttpUrl

/** Native login via HA's login flow API, bridge check and storage of the instance (concept 4.2, 5.1). */
class HaSignInRepository @Inject constructor(
    private val flows: LoginFlowClient,
    private val tokens: TokenClient,
    private val bridge: BridgeInfoClient,
    private val registry: InstanceRegistry,
) : SignInRepository {
    /** Reads the stored instance for the login of concept 4.1. */
    override suspend fun knownServer(serverId: String): KnownServer? = registry.find(serverId)?.let {
        KnownServer(it.id, it.baseUrl, it.displayName, it.haUserName)
    }

    /** A new flow per attempt, so a wrong password never leaves a half-used flow behind. */
    override suspend fun signIn(url: HttpUrl, username: String, password: CharArray): SignInStep {
        val flow = flows.start(url) as? LoginFlowStep.Form ?: throw UnexpectedException()
        return next(url, flows.submitCredentials(url, flow.flowId, username, password))
    }

    /** Sends the code; a wrong one keeps the flow, so the user can try again. */
    override suspend fun submitCode(url: HttpUrl, flowId: String, code: String): SignInStep =
        next(url, flows.submitMfaCode(url, flowId, code))

    /** Bridge check first: without HAAC Bridge the instance is not stored. */
    override suspend fun finish(target: SignInTarget, username: String, tokens: AuthTokens): String {
        val info = bridge.fetch(target.url, tokens.accessToken)
        return registry.save(target, username, info, tokens.refreshToken)
    }

    /** Best effort: an unrevoked token expires in HA after a period of inactivity (concept 5.2). */
    override suspend fun discard(url: HttpUrl, tokens: AuthTokens): Boolean = try {
        this.tokens.revoke(url, tokens.refreshToken)
        true
    } catch (_: HaacException) {
        false
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

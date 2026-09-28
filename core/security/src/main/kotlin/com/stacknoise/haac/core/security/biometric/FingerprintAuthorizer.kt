package com.stacknoise.haac.core.security.biometric

import android.security.keystore.UserNotAuthenticatedException
import javax.crypto.Cipher

/** Result of an operation that needed a fingerprint: its value, or why it did not run. */
internal sealed interface Authorized<out T> {
    /** The operation ran. */
    class Done<T>(val value: T) : Authorized<T>

    /** The user did not authenticate; nothing ran. */
    class Stopped(val outcome: FingerprintOutcome) : Authorized<Nothing>
}

/** One fingerprint step: why it is asked, the window of the key used and whether a fingerprint was just given. */
internal class AuthStep(val purpose: PromptPurpose, val unlockWindowSeconds: Int, val afterPrompt: Boolean = false)

/**
 * Runs one cipher operation with a fingerprint key (concept 5.4).
 *
 * A per-use key (window 0) is authorised through a `CryptoObject`. A time-bound key cannot be wrapped: its
 * cipher can only be created after a strong biometric, so the prompt comes first and the Keystore refuses the
 * key without it. After [AuthStep.afterPrompt] a fingerprint was just given, so a time-bound key is used
 * without a second prompt while the Keystore still accepts it.
 */
internal class FingerprintAuthorizer(private val prompter: BiometricPrompter) {
    /** Creates the cipher with [create], authorises it for [step] and applies [use]. */
    suspend fun <T> run(
        target: FingerprintTarget,
        step: AuthStep,
        create: () -> Cipher,
        use: (Cipher) -> T,
    ): Authorized<T> =
        if (step.unlockWindowSeconds == 0) perUse(target, step, create, use) else timeBound(target, step, create, use)

    /** Per-use key: the prompt carries the cipher as `CryptoObject`. */
    private suspend fun <T> perUse(
        target: FingerprintTarget,
        step: AuthStep,
        create: () -> Cipher,
        use: (Cipher) -> T,
    ): Authorized<T> {
        val cipher = create()
        return when (val result = prompt(target, step.purpose, cipher)) {
            is PromptResult.Success -> Authorized.Done(use(result.cipher ?: cipher))
            else -> Authorized.Stopped(outcome(result))
        }
    }

    /** Time-bound key: prompt first unless a fingerprint was just given and the Keystore still accepts it. */
    private suspend fun <T> timeBound(
        target: FingerprintTarget,
        step: AuthStep,
        create: () -> Cipher,
        use: (Cipher) -> T,
    ): Authorized<T> {
        val ready = if (step.afterPrompt) createIfAuthenticated(create) else null
        if (ready == null) {
            val result = prompt(target, step.purpose, cipher = null)
            if (result !is PromptResult.Success) return Authorized.Stopped(outcome(result))
        }
        return Authorized.Done(use(ready ?: create()))
    }

    /** The cipher of a time-bound key, or null if the Keystore wants a new fingerprint first. */
    private fun createIfAuthenticated(create: () -> Cipher): Cipher? = try {
        create()
    } catch (_: UserNotAuthenticatedException) {
        null
    }

    /** Shows the prompt for [target]. */
    private suspend fun prompt(target: FingerprintTarget, purpose: PromptPurpose, cipher: Cipher?): PromptResult =
        prompter.authenticate(target.activity, purpose, target.instanceName, cipher)

    /** *Use password* on the unlock prompt; every other end is a cancel. */
    private fun outcome(result: PromptResult): FingerprintOutcome =
        if (result == PromptResult.NegativeButton) FingerprintOutcome.USE_PASSWORD else FingerprintOutcome.CANCELLED
}

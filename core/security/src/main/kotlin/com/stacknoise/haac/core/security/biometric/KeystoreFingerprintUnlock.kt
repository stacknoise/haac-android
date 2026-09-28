package com.stacknoise.haac.core.security.biometric

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.token.SealedToken
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.TokenStore
import com.stacknoise.haac.core.security.token.UnlockedTokens
import com.stacknoise.haac.core.security.token.keystoreGuarded
import javax.inject.Inject

/** A decrypted refresh token and the fingerprint key it came from (null: the device-bound key). */
private class Opened(val token: String, val protection: TokenProtection.Fingerprint?)

/** [FingerprintUnlock] with Keystore keys; one [FingerprintVault] step per cipher operation. */
class KeystoreFingerprintUnlock @Inject constructor(
    private val vault: FingerprintVault,
    private val store: TokenStore,
    private val unlocked: UnlockedTokens,
    private val prompter: BiometricPrompter,
    private val errors: ErrorFactory,
) : FingerprintUnlock {
    private val authorizer = FingerprintAuthorizer(prompter)

    /** Class 3 with an enrolled finger. */
    override fun isAvailable(): Boolean = prompter.canAuthenticate()

    /** Keeps the token in memory; a changed window is applied right away, a cancelled re-key keeps the old key. */
    override suspend fun unlock(target: FingerprintTarget, unlockWindowSeconds: Int) = guarded(target) {
        when (val opened = open(target, PromptPurpose.UNLOCK)) {
            is Authorized.Stopped -> opened.outcome
            is Authorized.Done -> {
                unlocked.put(target.serverId, opened.value.token)
                if (opened.value.protection?.unlockWindowSeconds != unlockWindowSeconds) {
                    val step = AuthStep(PromptPurpose.CHANGE_WINDOW, unlockWindowSeconds, afterPrompt = true)
                    seal(target, opened.value, step)
                }
                FingerprintOutcome.DONE
            }
        }
    }

    /** Reads the token with the device-bound key and seals it with a new fingerprint key. */
    override suspend fun enable(target: FingerprintTarget, unlockWindowSeconds: Int) = guarded(target) {
        val token = store.read(target.serverId) ?: throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
        seal(target, Opened(token, protection = null), AuthStep(PromptPurpose.ENABLE, unlockWindowSeconds))
    }

    /** The token store writes it with the device-bound key and deletes the fingerprint key. */
    override suspend fun disable(target: FingerprintTarget) = guarded(target) {
        when (val opened = open(target, PromptPurpose.DISABLE)) {
            is Authorized.Stopped -> opened.outcome
            is Authorized.Done -> {
                store.save(target.serverId, opened.value.token)
                FingerprintOutcome.DONE
            }
        }
    }

    /** Uses the unlocked token if there is one, so one fingerprint check re-keys it. */
    override suspend fun changeUnlockWindow(target: FingerprintTarget, unlockWindowSeconds: Int) = guarded(target) {
        val token = unlocked.get(target.serverId)
        val opened = if (token != null) {
            Authorized.Done(Opened(token, vault.read(target.serverId).second))
        } else {
            open(target, PromptPurpose.CHANGE_WINDOW)
        }
        when (opened) {
            is Authorized.Stopped -> opened.outcome
            is Authorized.Done -> {
                val step = AuthStep(PromptPurpose.CHANGE_WINDOW, unlockWindowSeconds, afterPrompt = token == null)
                seal(target, opened.value, step)
            }
        }
    }

    /** Prompts for the fingerprint key of [target] and decrypts its token. */
    private suspend fun open(target: FingerprintTarget, purpose: PromptPurpose): Authorized<Opened> {
        val (token, protection) = vault.read(target.serverId)
        val step = AuthStep(purpose, protection.unlockWindowSeconds)
        return authorizer.run(target, step, { vault.decryptCipher(target.serverId, token, protection) }) { cipher ->
            val plain = cipher.doFinal(token.ciphertext)
            try {
                Opened(plain.decodeToString(), protection)
            } finally {
                plain.fill(0)
            }
        }
    }

    /** Encrypts [opened] with a new fingerprint key; the old key is deleted only after the new file is written. */
    private suspend fun seal(target: FingerprintTarget, opened: Opened, step: AuthStep): FingerprintOutcome {
        val serverId = target.serverId
        val protection = vault.newKey(serverId, opened.protection, step.unlockWindowSeconds)
        var committed = false
        try {
            val sealed = authorizer.run(target, step, { vault.encryptCipher(serverId, protection) }) { cipher ->
                val plain = opened.token.encodeToByteArray()
                try {
                    SealedToken(protection, cipher.iv, cipher.doFinal(plain))
                } finally {
                    plain.fill(0)
                }
            }
            when (sealed) {
                is Authorized.Stopped -> return sealed.outcome
                is Authorized.Done -> vault.commit(serverId, sealed.value, opened.protection)
            }
            committed = true
            unlocked.put(serverId, opened.token)
            return FingerprintOutcome.DONE
        } finally {
            if (!committed) vault.discard(serverId, protection)
        }
    }

    /**
     * Runs [block] with Keystore errors converted (concept 17.3). An invalidated fingerprint key deletes
     * token and keys, so the next start asks for the password (concept 5.4 step 5).
     */
    private suspend fun guarded(
        target: FingerprintTarget,
        block: suspend () -> FingerprintOutcome,
    ): FingerprintOutcome = try {
        keystoreGuarded(errors, block)
    } catch (e: KeystoreException) {
        if (e.code == ErrorCode.SEC_BIOMETRICS_CHANGED) store.delete(target.serverId)
        throw e
    }
}

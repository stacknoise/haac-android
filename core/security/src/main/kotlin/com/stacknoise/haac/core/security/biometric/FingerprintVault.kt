package com.stacknoise.haac.core.security.biometric

import com.stacknoise.haac.core.error.AuthException
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.CipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory
import com.stacknoise.haac.core.security.token.SealedToken
import com.stacknoise.haac.core.security.token.TokenFiles
import com.stacknoise.haac.core.security.token.TokenProtection
import javax.crypto.Cipher
import javax.inject.Inject

/**
 * Key, cipher and file steps of fingerprint protection (concept 5.4); the prompt is up to the caller.
 * A new key gets the next generation and replaces the old one only after the new file is written.
 */
class FingerprintVault @Inject constructor(
    private val files: TokenFiles,
    private val keys: KeyFactory,
    private val ciphers: CipherFactory,
) {
    /** The fingerprint-protected token of [serverId]; HAAC-AUTH-003 if there is none. */
    fun read(serverId: String): Pair<SealedToken, TokenProtection.Fingerprint> {
        val token = files.read(serverId)
        val protection = token?.protection as? TokenProtection.Fingerprint
        if (token == null || protection == null) throw AuthException(ErrorCode.AUTH_SESSION_EXPIRED)
        return token to protection
    }

    /** A decrypt cipher for [token]; HAAC-SEC-001 if its key is gone. */
    fun decryptCipher(serverId: String, token: SealedToken, protection: TokenProtection.Fingerprint): Cipher {
        val key = keys.fingerprintKey(serverId, protection.generation)
            ?: throw KeystoreException(ErrorCode.SEC_BIOMETRICS_CHANGED)
        return ciphers.decrypt(key, token.iv)
    }

    /** Creates the key after [previous] for [unlockWindowSeconds] and returns its protection. */
    fun newKey(
        serverId: String,
        previous: TokenProtection.Fingerprint?,
        unlockWindowSeconds: Int,
    ): TokenProtection.Fingerprint {
        val protection = TokenProtection.Fingerprint((previous?.generation ?: 0) + 1, unlockWindowSeconds)
        keys.newFingerprintKey(serverId, protection.generation, unlockWindowSeconds)
        return protection
    }

    /** An encrypt cipher for the key of [protection]. */
    fun encryptCipher(serverId: String, protection: TokenProtection.Fingerprint): Cipher {
        val key = keys.fingerprintKey(serverId, protection.generation)
            ?: throw KeystoreException(ErrorCode.SEC_STORAGE_UNAVAILABLE)
        return ciphers.encrypt(key)
    }

    /** Writes [token], then deletes the device-bound key and the fingerprint key [previous]. */
    fun commit(serverId: String, token: SealedToken, previous: TokenProtection.Fingerprint?) {
        files.write(serverId, token)
        keys.deleteTokenKey(serverId)
        previous?.let { keys.deleteFingerprintKey(serverId, it.generation) }
    }

    /** Deletes a new key that was not used. */
    fun discard(serverId: String, protection: TokenProtection.Fingerprint) =
        keys.deleteFingerprintKey(serverId, protection.generation)
}

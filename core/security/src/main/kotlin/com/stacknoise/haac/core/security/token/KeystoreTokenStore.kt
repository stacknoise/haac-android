package com.stacknoise.haac.core.security.token

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.CipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory

/**
 * Token files with the device-bound key (concept 5.3); fingerprint-protected tokens are read from
 * [UnlockedTokens] after the unlock (5.4). The key never leaves the Keystore; a file alone is useless on
 * another device.
 */
class KeystoreTokenStore(
    private val files: TokenFiles,
    private val keys: KeyFactory,
    private val ciphers: CipherFactory,
    private val unlocked: UnlockedTokens,
    private val errors: ErrorFactory,
) : TokenStore {
    /** Encrypts with the device-bound key, replaces the file, then drops the fingerprint keys it replaces. */
    override suspend fun save(serverId: String, refreshToken: String) = keystoreGuarded(errors) {
        val previous = files.read(serverId)?.protection
        val cipher = ciphers.encrypt(keys.tokenKey(serverId))
        val plain = refreshToken.encodeToByteArray()
        val sealed = try {
            cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
        files.write(serverId, SealedToken(TokenProtection.DeviceKey, cipher.iv, sealed))
        unlocked.remove(serverId)
        if (previous is TokenProtection.Fingerprint) keys.deleteFingerprintKey(serverId, previous.generation)
    }

    /** Decrypts a device-key token; a fingerprint token comes from memory or fails with HAAC-SEC-003. */
    override suspend fun read(serverId: String): String? = keystoreGuarded(errors) {
        val token = files.read(serverId) ?: return@keystoreGuarded null
        when (token.protection) {
            TokenProtection.DeviceKey -> decrypt(serverId, token)
            is TokenProtection.Fingerprint ->
                unlocked.get(serverId) ?: throw KeystoreException(ErrorCode.SEC_LOCKED)
        }
    }

    /** Checks only that the file exists. */
    override suspend fun contains(serverId: String): Boolean = keystoreGuarded(errors) { files.exists(serverId) }

    /** Reads only the file header. */
    override suspend fun protection(serverId: String): TokenProtection? =
        keystoreGuarded(errors) { files.read(serverId)?.protection }

    /** Removes file, unlocked copy and keys; missing ones are fine. */
    override suspend fun delete(serverId: String) = keystoreGuarded(errors) {
        files.delete(serverId)
        unlocked.remove(serverId)
        keys.deleteKeys(serverId)
    }

    /** Decrypts [token] with the device-bound key of [serverId] and wipes the plaintext bytes. */
    private fun decrypt(serverId: String, token: SealedToken): String {
        val plain = ciphers.decrypt(keys.tokenKey(serverId), token.iv).doFinal(token.ciphertext)
        return try {
            plain.decodeToString()
        } finally {
            plain.fill(0)
        }
    }
}

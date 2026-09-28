package com.stacknoise.haac.core.security.keystore

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/** Creates initialised ciphers for Keystore keys (concept 5.3, 17.2). */
interface CipherFactory {
    /** A cipher that encrypts with [key] and a fresh random IV. */
    fun encrypt(key: SecretKey): Cipher

    /** A cipher that decrypts with [key] and the stored [iv]. */
    fun decrypt(key: SecretKey, iv: ByteArray): Cipher
}

/** AES-GCM without padding and a 128-bit tag. */
class AesGcmCipherFactory @Inject constructor() : CipherFactory {
    /** Lets the provider choose the IV, as the Keystore requires. */
    override fun encrypt(key: SecretKey): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }

    /** Uses the IV that was stored next to the ciphertext. */
    override fun decrypt(key: SecretKey, iv: ByteArray): Cipher =
        Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv)) }

    /** Cipher parameters of concept 5.3. */
    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}

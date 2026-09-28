package com.stacknoise.haac.core.security.keystore

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

/**
 * Creates and deletes the Keystore keys of an instance; the key parameters of concept 5.3 and 5.4 live only
 * here (17.2). A fingerprint key has a generation, so a new key never replaces the one the stored token uses.
 */
interface KeyFactory {
    /** Returns the device-bound AES-GCM key of [serverId] (concept 5.3), creating it on first use. */
    fun tokenKey(serverId: String): SecretKey

    /** Returns the fingerprint key [generation] of [serverId], or null if it does not exist (concept 5.4). */
    fun fingerprintKey(serverId: String, generation: Int): SecretKey?

    /** Creates fingerprint key [generation] of [serverId]; 0 [unlockWindowSeconds] means one fingerprint per use. */
    fun newFingerprintKey(serverId: String, generation: Int, unlockWindowSeconds: Int): SecretKey

    /** Deletes the device-bound key of [serverId]; does nothing if none exists. */
    fun deleteTokenKey(serverId: String)

    /** Deletes fingerprint key [generation] of [serverId]; does nothing if none exists. */
    fun deleteFingerprintKey(serverId: String, generation: Int)

    /** Deletes every key of [serverId]; does nothing if none exists. */
    fun deleteKeys(serverId: String)
}

/** Keys in the Android Keystore, StrongBox-backed when the device has it (concept 5.3). */
class AndroidKeyFactory @Inject constructor() : KeyFactory {
    /** Loads the key of [serverId] or generates it. */
    override fun tokenKey(serverId: String): SecretKey {
        val alias = tokenAlias(serverId)
        (keyStore().getKey(alias, null) as? SecretKey)?.let { return it }
        return generate(alias) {}
    }

    /** Loads the fingerprint key; null after the Keystore lost it. */
    override fun fingerprintKey(serverId: String, generation: Int): SecretKey? =
        keyStore().getKey(fingerprintAlias(serverId, generation), null) as? SecretKey

    /**
     * Bound to a Class-3 biometric and invalidated when fingerprints are added or removed (concept 5.4).
     * Before Android 11 only per-use keys are offered, because the time-bound variant there also accepts
     * the device PIN.
     */
    override fun newFingerprintKey(serverId: String, generation: Int, unlockWindowSeconds: Int): SecretKey {
        val alias = fingerprintAlias(serverId, generation)
        delete(alias)
        return generate(alias) {
            setUserAuthenticationRequired(true)
            setInvalidatedByBiometricEnrollment(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setUserAuthenticationParameters(unlockWindowSeconds, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION") // the replacement needs API 30
                setUserAuthenticationValidityDurationSeconds(PER_USE_BEFORE_R)
            }
        }
    }

    /** Removes the device-bound key. */
    override fun deleteTokenKey(serverId: String) = delete(tokenAlias(serverId))

    /** Removes one fingerprint key. */
    override fun deleteFingerprintKey(serverId: String, generation: Int) =
        delete(fingerprintAlias(serverId, generation))

    /** Removes the device-bound key and every fingerprint key of [serverId]. */
    override fun deleteKeys(serverId: String) {
        val store = keyStore()
        val prefix = fingerprintAlias(serverId, generation = null)
        val aliases = store.aliases().toList().filter { it == tokenAlias(serverId) || it.startsWith(prefix) }
        aliases.forEach(store::deleteEntry)
    }

    /**
     * AES-256-GCM, encrypt/decrypt only, usable only while the device is unlocked (concept 5.3); [configure]
     * adds the user authentication. Falls back to the TEE when the device has no StrongBox.
     */
    private fun generate(
        alias: String,
        strongBox: Boolean = true,
        configure: KeyGenParameterSpec.Builder.() -> Unit,
    ): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUnlockedDeviceRequired(true)
            .setIsStrongBoxBacked(strongBox)
            .apply(configure)
            .build()
        return try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
                init(spec)
                generateKey()
            }
        } catch (e: StrongBoxUnavailableException) {
            if (strongBox) generate(alias, strongBox = false, configure) else throw e
        }
    }

    /** Deletes [alias] if it exists. */
    private fun delete(alias: String) {
        val store = keyStore()
        if (store.containsAlias(alias)) store.deleteEntry(alias)
    }

    /** The loaded Android Keystore. */
    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Keystore provider, key parameters and aliases. */
    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_BITS = 256

        /** `-1` before API 30 means: authentication for every use, which requires a CryptoObject. */
        const val PER_USE_BEFORE_R = -1

        /** Keystore alias of the device-bound key of [serverId]. */
        fun tokenAlias(serverId: String) = "haac.token.$serverId"

        /** Keystore alias of fingerprint key [generation] of [serverId]; without generation the common prefix. */
        fun fingerprintAlias(serverId: String, generation: Int?) =
            "haac.fingerprint.$serverId." + (generation?.toString() ?: "")
    }
}

package com.stacknoise.haac.core.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject

/** Creates and deletes the Keystore keys of an instance; the key parameters of concept 5.3 live only here (17.2). */
interface KeyFactory {
    /** Returns the AES-GCM key that encrypts the refresh token of [serverId], creating it on first use. */
    fun tokenKey(serverId: String): SecretKey

    /** Deletes every key of [serverId]; does nothing if none exists. */
    fun deleteKeys(serverId: String)
}

/** Keys in the Android Keystore, StrongBox-backed when the device has it (concept 5.3). */
class AndroidKeyFactory @Inject constructor() : KeyFactory {
    /** Loads the key of [serverId] or generates it. */
    override fun tokenKey(serverId: String): SecretKey {
        val alias = alias(serverId)
        (keyStore().getKey(alias, null) as? SecretKey)?.let { return it }
        return try {
            generate(alias, strongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generate(alias, strongBox = false)
        }
    }

    /** Removes the token key of [serverId] from the Keystore. */
    override fun deleteKeys(serverId: String) {
        val store = keyStore()
        val alias = alias(serverId)
        if (store.containsAlias(alias)) store.deleteEntry(alias)
    }

    /** AES-256-GCM, encrypt/decrypt only, usable only while the device is unlocked. */
    private fun generate(alias: String, strongBox: Boolean): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUnlockedDeviceRequired(true)
            .setIsStrongBoxBacked(strongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(spec)
            generateKey()
        }
    }

    /** The loaded Android Keystore. */
    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** Keystore alias of the token key of [serverId]. */
    private fun alias(serverId: String) = "haac.token.$serverId"

    /** Keystore provider and key size. */
    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_BITS = 256
    }
}

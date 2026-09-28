package com.stacknoise.haac.core.security.keystore

import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** Software AES keys instead of the Android Keystore; keys are listed by alias for assertions. */
class SoftwareKeyFactory : KeyFactory {
    val keys = mutableMapOf<String, SecretKey>()

    override fun tokenKey(serverId: String): SecretKey = keys.getOrPut("token.$serverId") { newKey() }

    override fun fingerprintKey(serverId: String, generation: Int): SecretKey? = keys["fp.$serverId.$generation"]

    override fun newFingerprintKey(serverId: String, generation: Int, unlockWindowSeconds: Int): SecretKey =
        newKey().also { keys["fp.$serverId.$generation"] = it }

    override fun deleteTokenKey(serverId: String) {
        keys.remove("token.$serverId")
    }

    override fun deleteFingerprintKey(serverId: String, generation: Int) {
        keys.remove("fp.$serverId.$generation")
    }

    override fun deleteKeys(serverId: String) {
        keys.keys.removeAll { it == "token.$serverId" || it.startsWith("fp.$serverId.") }
    }

    /** Replaces the device-bound key, so the stored token no longer decrypts. */
    fun replaceTokenKey(serverId: String) {
        keys["token.$serverId"] = newKey()
    }

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
}

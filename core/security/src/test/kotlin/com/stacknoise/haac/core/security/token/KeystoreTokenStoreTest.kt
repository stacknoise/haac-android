package com.stacknoise.haac.core.security.token

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.AesGcmCipherFactory
import com.stacknoise.haac.core.security.keystore.KeyFactory
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class KeystoreTokenStoreTest {
    @TempDir
    lateinit var dir: File

    /** Software AES keys instead of the Android Keystore. */
    private val keys = object : KeyFactory {
        val created = mutableMapOf<String, SecretKey>()

        override fun tokenKey(serverId: String): SecretKey = created.getOrPut(serverId) {
            KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        }

        override fun deleteKeys(serverId: String) {
            created.remove(serverId)
        }
    }

    private fun store() = KeystoreTokenStore(dir, keys, AesGcmCipherFactory(), DefaultErrorFactory())

    @Test
    fun `stores the token encrypted and reads it back`() = runTest {
        val store = store()
        store.save("a", "secret-refresh-token")
        assertTrue(store.contains("a"))
        assertEquals("secret-refresh-token", store.read("a"))
        val raw = File(dir, "a.bin").readBytes().decodeToString(throwOnInvalidSequence = false)
        assertFalse(raw.contains("secret-refresh-token"))
    }

    @Test
    fun `instances are separate and delete removes file and key`() = runTest {
        val store = store()
        store.save("a", "token-a")
        store.save("b", "token-b")
        store.delete("a")
        assertNull(store.read("a"))
        assertFalse(store.contains("a"))
        assertFalse("a" in keys.created)
        assertEquals("token-b", store.read("b"))
    }

    @Test
    fun `a token encrypted with another key cannot be read`() = runTest {
        val store = store()
        store.save("a", "token-a")
        keys.created["a"] = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val error = assertThrows<KeystoreException> { store.read("a") }
        assertEquals(ErrorCode.SEC_STORAGE_UNAVAILABLE, error.code)
    }
}

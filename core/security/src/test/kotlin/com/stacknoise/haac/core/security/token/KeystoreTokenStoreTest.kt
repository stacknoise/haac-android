package com.stacknoise.haac.core.security.token

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.AesGcmCipherFactory
import com.stacknoise.haac.core.security.keystore.SoftwareKeyFactory
import java.io.File
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

    private val keys = SoftwareKeyFactory()
    private val unlocked = UnlockedTokens()

    private fun store() =
        KeystoreTokenStore(TokenFiles(dir), keys, AesGcmCipherFactory(), unlocked, DefaultErrorFactory())

    @Test
    fun `stores the token encrypted and reads it back`() = runTest {
        val store = store()
        store.save("a", "secret-refresh-token")
        assertTrue(store.contains("a"))
        assertEquals(TokenProtection.DeviceKey, store.protection("a"))
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
        assertNull(store.protection("a"))
        assertFalse(store.contains("a"))
        assertFalse("token.a" in keys.keys)
        assertEquals("token-b", store.read("b"))
    }

    @Test
    fun `a token encrypted with another key cannot be read`() = runTest {
        val store = store()
        store.save("a", "token-a")
        keys.replaceTokenKey("a")
        val error = assertThrows<KeystoreException> { store.read("a") }
        assertEquals(ErrorCode.SEC_STORAGE_UNAVAILABLE, error.code)
    }

    @Test
    fun `a fingerprint token is read only while unlocked`() = runTest {
        val store = store()
        TokenFiles(dir).write("a", SealedToken(TokenProtection.Fingerprint(1, 0), ByteArray(12), ByteArray(16)))
        assertEquals(ErrorCode.SEC_LOCKED, assertThrows<KeystoreException> { store.read("a") }.code)
        unlocked.put("a", "token-a")
        assertEquals("token-a", store.read("a"))
        store.delete("a")
        assertNull(unlocked.get("a"))
    }

    @Test
    fun `saving with the device key replaces a fingerprint token and its key`() = runTest {
        val store = store()
        keys.newFingerprintKey("a", 3, 0)
        TokenFiles(dir).write("a", SealedToken(TokenProtection.Fingerprint(3, 0), ByteArray(12), ByteArray(16)))
        unlocked.put("a", "old")
        store.save("a", "new")
        assertEquals(TokenProtection.DeviceKey, store.protection("a"))
        assertFalse("fp.a.3" in keys.keys)
        assertNull(unlocked.get("a"))
        assertEquals("new", store.read("a"))
    }
}

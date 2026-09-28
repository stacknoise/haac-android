package com.stacknoise.haac.core.security.biometric

import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.keystore.AesGcmCipherFactory
import com.stacknoise.haac.core.security.keystore.SoftwareKeyFactory
import com.stacknoise.haac.core.security.token.KeystoreTokenStore
import com.stacknoise.haac.core.security.token.TokenFiles
import com.stacknoise.haac.core.security.token.TokenProtection
import com.stacknoise.haac.core.security.token.UnlockedTokens
import io.mockk.mockk
import java.io.File
import javax.crypto.Cipher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class KeystoreFingerprintUnlockTest {
    @TempDir
    lateinit var dir: File

    private val keys = SoftwareKeyFactory()
    private val unlocked = UnlockedTokens()
    private val target = FingerprintTarget(mockk<FragmentActivity>(), "a", "Home")

    /** Answers every prompt with [answer] and records the cipher it was given. */
    private val prompter = object : BiometricPrompter {
        var answer: PromptResult? = null
        val ciphers = mutableListOf<Cipher?>()

        override fun canAuthenticate() = true

        override suspend fun authenticate(
            activity: FragmentActivity,
            purpose: PromptPurpose,
            instanceName: String,
            cipher: Cipher?,
        ): PromptResult {
            ciphers += cipher
            return answer ?: PromptResult.Success(cipher)
        }
    }

    private lateinit var files: TokenFiles
    private lateinit var store: KeystoreTokenStore
    private lateinit var unlock: KeystoreFingerprintUnlock

    @BeforeEach
    fun setUp() = runTest {
        files = TokenFiles(dir)
        val ciphers = AesGcmCipherFactory()
        store = KeystoreTokenStore(files, keys, ciphers, unlocked, DefaultErrorFactory())
        unlock = KeystoreFingerprintUnlock(
            FingerprintVault(files, keys, ciphers),
            store,
            unlocked,
            prompter,
            DefaultErrorFactory(),
        )
        store.save("a", "refresh-a")
    }

    @Test
    fun `enabling moves the token to a fingerprint key with a CryptoObject`() = runTest {
        assertEquals(FingerprintOutcome.DONE, unlock.enable(target, 0))
        assertEquals(TokenProtection.Fingerprint(1, 0), store.protection("a"))
        assertNotNull(prompter.ciphers.single())
        assertFalse("token.a" in keys.keys)
        assertEquals("refresh-a", store.read("a"))
    }

    @Test
    fun `after a lock the token needs the fingerprint again`() = runTest {
        unlock.enable(target, 0)
        unlocked.clear()
        assertEquals(ErrorCode.SEC_LOCKED, assertThrows<KeystoreException> { store.read("a") }.code)
        assertEquals(FingerprintOutcome.DONE, unlock.unlock(target, 0))
        assertEquals("refresh-a", store.read("a"))
    }

    @Test
    fun `cancelling keeps the old key and drops the new one`() = runTest {
        prompter.answer = PromptResult.Cancelled
        assertEquals(FingerprintOutcome.CANCELLED, unlock.enable(target, 0))
        assertEquals(TokenProtection.DeviceKey, store.protection("a"))
        assertFalse(keys.keys.keys.any { it.startsWith("fp.") })
        assertEquals("refresh-a", store.read("a"))
    }

    @Test
    fun `use password on the unlock prompt leaves the token locked`() = runTest {
        unlock.enable(target, 0)
        unlocked.clear()
        prompter.answer = PromptResult.NegativeButton
        assertEquals(FingerprintOutcome.USE_PASSWORD, unlock.unlock(target, 0))
        assertNull(unlocked.get("a"))
        assertTrue(store.contains("a"))
    }

    @Test
    fun `disabling returns to the device key`() = runTest {
        unlock.enable(target, 0)
        unlocked.clear()
        assertEquals(FingerprintOutcome.DONE, unlock.disable(target))
        assertEquals(TokenProtection.DeviceKey, store.protection("a"))
        assertFalse(keys.keys.keys.any { it.startsWith("fp.") })
        assertEquals("refresh-a", store.read("a"))
    }

    @Test
    fun `a new window re-keys the unlocked token with one prompt without CryptoObject`() = runTest {
        unlock.enable(target, 0)
        prompter.ciphers.clear()
        assertEquals(FingerprintOutcome.DONE, unlock.changeUnlockWindow(target, 300))
        assertEquals(TokenProtection.Fingerprint(2, 300), store.protection("a"))
        assertEquals(listOf<Cipher?>(null), prompter.ciphers)
        assertEquals(setOf("fp.a.2"), keys.keys.keys)
        unlocked.clear()
        assertEquals(FingerprintOutcome.DONE, unlock.unlock(target, 300))
        assertEquals("refresh-a", store.read("a"))
    }

    @Test
    fun `unlocking with another window setting re-keys afterwards`() = runTest {
        unlock.enable(target, 0)
        unlocked.clear()
        assertEquals(FingerprintOutcome.DONE, unlock.unlock(target, 60))
        assertEquals(TokenProtection.Fingerprint(2, 60), store.protection("a"))
    }

    @Test
    fun `a lost fingerprint key deletes the token and asks for the password`() = runTest {
        unlock.enable(target, 0)
        unlocked.clear()
        keys.deleteFingerprintKey("a", 1)
        val error = assertThrows<KeystoreException> { unlock.unlock(target, 0) }
        assertEquals(ErrorCode.SEC_BIOMETRICS_CHANGED, error.code)
        assertFalse(store.contains("a"))
    }
}

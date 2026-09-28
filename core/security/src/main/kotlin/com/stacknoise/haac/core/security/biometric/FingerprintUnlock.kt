package com.stacknoise.haac.core.security.biometric

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.fragment.app.FragmentActivity

/** The instance a fingerprint step is for, and the activity that shows the prompt. */
class FingerprintTarget(val activity: FragmentActivity, val serverId: String, val instanceName: String)

/** How a fingerprint step ended; errors are thrown as [com.stacknoise.haac.core.error.HaacException]. */
enum class FingerprintOutcome {
    /** Done: the token is unlocked or re-encrypted. */
    DONE,

    /** The user chose *Use password* on the unlock prompt (concept 5.4 step 6). */
    USE_PASSWORD,

    /** The user cancelled; nothing changed. */
    CANCELLED,
}

/**
 * Fingerprint unlock of the stored refresh token (concept 5.4). Unlocked tokens stay in memory until the
 * app locks (5.5). HAAC-SEC-001 means the fingerprint key was invalidated: token and keys are deleted and
 * the user has to sign in with the password again.
 */
interface FingerprintUnlock {
    /** True if the device has a Class-3 biometric with an enrolled finger. */
    fun isAvailable(): Boolean

    /** The unlock window needs time-bound keys that accept only biometrics, available from Android 11. */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val supportsUnlockWindow: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /**
     * Prompts and decrypts the token of [target]. If the token was sealed with another window than
     * [unlockWindowSeconds], it is re-encrypted with a new key afterwards (concept 5.4, window change).
     */
    suspend fun unlock(target: FingerprintTarget, unlockWindowSeconds: Int): FingerprintOutcome

    /** Moves the token of [target] from the device-bound key to a new fingerprint key. */
    suspend fun enable(target: FingerprintTarget, unlockWindowSeconds: Int): FingerprintOutcome

    /** After one last fingerprint, moves the token back to the device-bound key and deletes the fingerprint key. */
    suspend fun disable(target: FingerprintTarget): FingerprintOutcome

    /** Re-encrypts the token of [target] with a key for [unlockWindowSeconds]; one fingerprint check. */
    suspend fun changeUnlockWindow(target: FingerprintTarget, unlockWindowSeconds: Int): FingerprintOutcome
}

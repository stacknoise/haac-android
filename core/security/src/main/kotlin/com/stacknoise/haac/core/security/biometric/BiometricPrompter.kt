package com.stacknoise.haac.core.security.biometric

import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/** Why the fingerprint is asked for; decides the title and the negative button of the prompt. */
enum class PromptPurpose {
    /** Unlock at start or after the app lock; the negative button is *Use password* (concept 5.4 step 6). */
    UNLOCK,

    /** Settings → Security: turn fingerprint unlock on. */
    ENABLE,

    /** Settings → Security: turn fingerprint unlock off. */
    DISABLE,

    /** Settings → Security: new unlock window. */
    CHANGE_WINDOW,
}

/** How a prompt ended; errors such as too many attempts are thrown as HAAC-SEC-004 instead. */
sealed interface PromptResult {
    /** Fingerprint accepted; [cipher] is the authenticated cipher if one was passed. */
    class Success(val cipher: Cipher?) : PromptResult

    /** The user tapped the negative button (*Use password* or *Cancel*). */
    data object NegativeButton : PromptResult

    /** The prompt was dismissed or cancelled, e.g. by Back or because the app went to the background. */
    data object Cancelled : PromptResult
}

/** Shows the system fingerprint prompt, Class 3 only (concept 5.4). */
interface BiometricPrompter {
    /** True if the device has a Class-3 biometric with at least one enrolled finger. */
    fun canAuthenticate(): Boolean

    /**
     * Asks for a fingerprint for the instance [instanceName]. With a [cipher] the prompt carries it as
     * `CryptoObject`, so only a successful fingerprint makes the cipher usable.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        purpose: PromptPurpose,
        instanceName: String,
        cipher: Cipher?,
    ): PromptResult
}

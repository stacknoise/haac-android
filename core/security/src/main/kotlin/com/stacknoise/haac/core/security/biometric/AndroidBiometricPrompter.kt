package com.stacknoise.haac.core.security.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON
import androidx.biometric.BiometricPrompt.ERROR_TIMEOUT
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.KeystoreException
import com.stacknoise.haac.core.security.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** [BiometricPrompter] with AndroidX `BiometricPrompt`; the prompt is cancelled with the calling coroutine. */
class AndroidBiometricPrompter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : BiometricPrompter {
    /** Asks `BiometricManager` for Class 3. */
    override fun canAuthenticate(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    /** Shows the prompt on the main thread and waits for its result. */
    override suspend fun authenticate(
        activity: FragmentActivity,
        purpose: PromptPurpose,
        instanceName: String,
        cipher: Cipher?,
    ): PromptResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback(continuation))
            val info = promptInfo(purpose, instanceName)
            if (cipher == null) {
                prompt.authenticate(info)
            } else {
                prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
            }
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }

    /** Title per purpose, the instance as subtitle, Class 3 only and no extra confirmation tap. */
    private fun promptInfo(purpose: PromptPurpose, instanceName: String): BiometricPrompt.PromptInfo {
        val title = when (purpose) {
            PromptPurpose.UNLOCK -> R.string.fingerprint_prompt_unlock
            PromptPurpose.ENABLE -> R.string.fingerprint_prompt_enable
            PromptPurpose.DISABLE -> R.string.fingerprint_prompt_disable
            PromptPurpose.CHANGE_WINDOW -> R.string.fingerprint_prompt_change_window
        }
        val negative = when (purpose) {
            PromptPurpose.UNLOCK -> R.string.fingerprint_use_password
            else -> R.string.fingerprint_cancel
        }
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(title))
            .setSubtitle(instanceName)
            .setNegativeButtonText(context.getString(negative))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
    }

    /** Resumes [continuation] once; a failed finger only shakes the prompt and is not reported. */
    private fun callback(continuation: CancellableContinuation<PromptResult>) =
        object : BiometricPrompt.AuthenticationCallback() {
            /** Hands over the authenticated cipher of the CryptoObject, if any. */
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(PromptResult.Success(result.cryptoObject?.cipher))
            }

            /** Negative button and cancel end normally; lockout, no enrolled finger etc. become HAAC-SEC-004. */
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                when (errorCode) {
                    ERROR_NEGATIVE_BUTTON -> continuation.resume(PromptResult.NegativeButton)
                    ERROR_USER_CANCELED, ERROR_CANCELED, ERROR_TIMEOUT -> continuation.resume(PromptResult.Cancelled)
                    else -> continuation.resumeWithException(KeystoreException(ErrorCode.SEC_BIOMETRIC_UNAVAILABLE))
                }
            }
        }
}

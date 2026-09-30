package com.stacknoise.haac.feature.onboarding.data

import androidx.fragment.app.FragmentActivity
import com.stacknoise.haac.core.database.settings.SecuritySettings
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.security.biometric.FingerprintOutcome
import com.stacknoise.haac.core.security.biometric.FingerprintTarget
import com.stacknoise.haac.core.security.biometric.FingerprintUnlock
import com.stacknoise.haac.feature.onboarding.domain.FingerprintStep
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Enables fingerprint unlock with the unlock window of the security settings (concept 5.4). */
class BiometricFingerprintStep @Inject constructor(
    private val fingerprint: FingerprintUnlock,
    private val security: SecuritySettings,
) : FingerprintStep {
    /** Asks the biometric manager. */
    override fun available(): Boolean = fingerprint.isAvailable()

    /** A cancelled prompt or a failure leaves fingerprint unlock off; the settings can turn it on later. */
    override suspend fun enable(activity: FragmentActivity, serverId: String, name: String): Boolean = try {
        val window = security.unlockWindowMinutes.first() * SECONDS_PER_MINUTE
        fingerprint.enable(FingerprintTarget(activity, serverId, name), window) == FingerprintOutcome.DONE
    } catch (_: HaacException) {
        false
    }

    /** Time conversion. */
    private companion object {
        const val SECONDS_PER_MINUTE = 60
    }
}

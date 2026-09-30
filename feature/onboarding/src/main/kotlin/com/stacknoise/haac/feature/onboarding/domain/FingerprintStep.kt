package com.stacknoise.haac.feature.onboarding.domain

import androidx.fragment.app.FragmentActivity

/** The optional fingerprint step after a sign-in (concept 4.4, 5.4). */
interface FingerprintStep {
    /** True if the device has an enrolled Class-3 biometric, so the step can be offered. */
    fun available(): Boolean

    /** Turns fingerprint unlock on for the stored instance [serverId]; true if it is on afterwards. */
    suspend fun enable(activity: FragmentActivity, serverId: String, name: String): Boolean
}

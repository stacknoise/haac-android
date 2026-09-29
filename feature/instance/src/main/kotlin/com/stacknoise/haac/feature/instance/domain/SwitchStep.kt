package com.stacknoise.haac.feature.instance.domain

/** What must happen before the main area of another instance opens (concept 4.4). */
enum class SwitchStep {
    /** The instance is active now; its token is available without a prompt. */
    READY,

    /** The token is fingerprint-protected and locked: the unlock screen follows, the switch is not done yet. */
    UNLOCK,

    /** The instance has no token: the HA login follows. */
    SIGN_IN,
}

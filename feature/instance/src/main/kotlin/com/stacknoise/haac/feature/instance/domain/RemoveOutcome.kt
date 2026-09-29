package com.stacknoise.haac.feature.instance.domain

/** What follows the removal of an instance (concept 4.4). */
sealed interface RemoveOutcome {
    /** An inactive instance was removed; nothing changes on screen. */
    data object Kept : RemoveOutcome

    /** The active instance was removed: [serverId], the most recently used one, is active now and needs [step]. */
    data class Next(val serverId: String, val step: SwitchStep) : RemoveOutcome

    /** The last instance was removed: the app returns to the first-start screen. */
    data object NoneLeft : RemoveOutcome
}

package com.stacknoise.haac.core.error

/**
 * Receives every error that is not handled on screen and turns it into an entry of the
 * notification list (concept 17.4). Implemented by `:feature:notifications`.
 */
interface ErrorReporter {
    /** Records [error] for the instance [serverId], or as a global entry when it is null. */
    fun report(error: HaacException, serverId: String?)
}

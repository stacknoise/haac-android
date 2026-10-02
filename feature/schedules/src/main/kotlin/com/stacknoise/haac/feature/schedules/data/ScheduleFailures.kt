package com.stacknoise.haac.feature.schedules.data

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * Runs a schedule command for a screen and turns a failure into its error code (concept 17.4, 19.7): the error
 * also goes to the notification list, and a schedule that was changed somewhere else (HAAC-SCH-004) makes the
 * sync read the list again.
 */
@Singleton
class ScheduleFailures @Inject constructor(
    private val reporter: ErrorReporter,
    private val sync: ScheduleSync,
) {
    /** Runs [block] for instance [serverId]; returns null on success, else the code of the failure. */
    suspend fun run(serverId: String, block: suspend () -> Unit): ErrorCode? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: HaacException) {
        reporter.report(e, serverId)
        if (e.code == ErrorCode.SCH_CONFLICT) sync.recheck()
        e.code
    }
}

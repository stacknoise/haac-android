package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Failed service calls (concept 14.1, 17.4): shown on screen as a snackbar with the code, added to the
 * notification list, and a rejection by the bridge runs the revision check again.
 */
@Singleton
class ControlFailures internal constructor(private val reporter: ErrorReporter, private val recheck: () -> Unit) {
    /** Checks the revision with the running sync. */
    @Inject
    constructor(reporter: ErrorReporter, sync: EntitySync) : this(reporter, sync::recheck)

    private val events = MutableSharedFlow<HaacException>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Failures for the snackbar of the room grid. */
    val failures: SharedFlow<HaacException> = events.asSharedFlow()

    /** Handles [error] of a call on instance [serverId]. */
    fun handle(serverId: String, error: HaacException) {
        reporter.report(error, serverId)
        events.tryEmit(error)
        if (error.code in RECHECK) recheck()
    }

    /** Codes that mean the exposure may have changed (HAB-SVC-001, HAB-SVC-002, HAB-ENT-001). */
    private companion object {
        val RECHECK = setOf(ErrorCode.BRG_NOT_ALLOWED, ErrorCode.BRG_ACTION_NOT_AVAILABLE, ErrorCode.ENT_NOT_FOUND)
    }
}

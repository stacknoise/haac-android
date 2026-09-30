package com.stacknoise.haac.app.crash

import android.content.Context
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.UnexpectedException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records that the app died from an uncaught exception and reports it as HAAC-APP-000 at the next start
 * (concept 17.4). Only a marker file is written while the process is dying; no stack trace or exception message is
 * stored, because those can contain addresses or tokens. The trace is still in logcat and in Play's crash reports.
 */
@Singleton
class CrashMarker internal constructor(private val file: File) {
    /** Marker file in the app's private storage. */
    @Inject
    constructor(@ApplicationContext context: Context) : this(File(context.filesDir, MARKER_FILE))

    /**
     * Makes every uncaught exception leave a marker before the previous handler (the system's, which ends the app)
     * runs. Writing never throws, so it cannot hide the original crash.
     */
    fun install() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            mark()
            previous?.uncaughtException(thread, error)
        }
    }

    /** Reports the crash of the last run, if there was one, as HAAC-APP-000 without an instance; then forgets it. */
    fun replayTo(reporter: ErrorReporter) {
        if (consume()) reporter.report(UnexpectedException(), null)
    }

    /** Writes the marker with the time of the crash. */
    internal fun mark() {
        @Suppress("TooGenericExceptionCaught") // Nothing may be thrown while the process is dying.
        try {
            file.writeText(System.currentTimeMillis().toString())
        } catch (_: Throwable) {
            // The crash is reported by the system anyway.
        }
    }

    /** True once if a marker existed; it is deleted. */
    internal fun consume(): Boolean = file.exists() && file.delete()

    /** File name of the marker. */
    private companion object {
        const val MARKER_FILE = "crash.marker"
    }
}

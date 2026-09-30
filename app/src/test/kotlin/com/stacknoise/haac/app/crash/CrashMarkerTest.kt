package com.stacknoise.haac.app.crash

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorReporter
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CrashMarkerTest {
    @TempDir
    lateinit var dir: File

    private val original = Thread.getDefaultUncaughtExceptionHandler()
    private val reported = mutableListOf<Pair<ErrorCode, String?>>()
    private val reporter = ErrorReporter { error, serverId ->
        reported += error.code to serverId
    }

    @AfterEach
    fun restore() = Thread.setDefaultUncaughtExceptionHandler(original)

    private fun marker() = CrashMarker(File(dir, "crash.marker"))

    @Test
    fun `an uncaught exception leaves a marker and still reaches the previous handler`() {
        var seen: Throwable? = null
        Thread.setDefaultUncaughtExceptionHandler { _, error -> seen = error }
        val marker = marker()
        marker.install()

        val boom = IllegalStateException("boom")
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(Thread.currentThread(), boom)

        assertEquals(boom, seen)
        assertTrue(File(dir, "crash.marker").exists())
        assertFalse(File(dir, "crash.marker").readText().contains("boom"))
    }

    @Test
    fun `the next start reports HAAC-APP-000 once`() {
        val marker = marker()
        marker.mark()
        marker.replayTo(reporter)
        marker.replayTo(reporter)
        assertEquals(listOf<Pair<ErrorCode, String?>>(ErrorCode.APP_UNEXPECTED to null), reported)
    }

    @Test
    fun `without a crash nothing is reported`() {
        marker().replayTo(reporter)
        assertTrue(reported.isEmpty())
    }
}

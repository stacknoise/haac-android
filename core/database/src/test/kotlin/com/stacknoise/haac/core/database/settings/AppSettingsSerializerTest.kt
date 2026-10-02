package com.stacknoise.haac.core.database.settings

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Reads and writes [AppSettings] through [AppSettingsSerializer]. */
class AppSettingsSerializerTest {
    private fun read(json: String): AppSettings =
        runBlocking { AppSettingsSerializer.readFrom(ByteArrayInputStream(json.encodeToByteArray())) }

    @Test
    fun `a settings file from before the design mode follows the system`() {
        val settings = read("""{"activeServerId":"s1","unlockWindowMinutes":5,"lockTimeoutMinutes":15}""")

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals("s1", settings.activeServerId)
        assertEquals(15, settings.lockTimeoutMinutes)
    }

    @Test
    fun `the design mode survives a write and a read`() {
        val out = ByteArrayOutputStream()
        runBlocking { AppSettingsSerializer.writeTo(AppSettings(themeMode = ThemeMode.DARK), out) }

        assertEquals(ThemeMode.DARK, read(out.toString(Charsets.UTF_8)).themeMode)
    }
}

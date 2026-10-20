package com.stacknoise.haac.core.database.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** [DeviceName.clean]: only what the bridge accepts leaves the app (concept 21.5). */
class DeviceNameTest {
    @Test
    fun `a plain name stays as it is`() {
        assertEquals("Pixel 8", DeviceName.clean("Pixel 8"))
    }

    @Test
    fun `blanks around the name and control characters are removed`() {
        assertEquals("Pixel 8", DeviceName.clean("  Pixel\u0000 8\n"))
    }

    @Test
    fun `line and paragraph separators are removed and other spaces become a space`() {
        assertEquals("AB", DeviceName.clean("A  B"))
        assertEquals("A B", DeviceName.clean("A B"))
    }

    @Test
    fun `a name without printable characters is nothing`() {
        assertNull(DeviceName.clean(""))
        assertNull(DeviceName.clean(" \t\n "))
    }

    @Test
    fun `a long name is cut to forty characters`() {
        assertEquals("a".repeat(DeviceName.MAX_LENGTH), DeviceName.clean("a".repeat(100)))
    }

    @Test
    fun `a cut through an emoji does not leave half of it`() {
        val cleaned = DeviceName.clean("a".repeat(DeviceName.MAX_LENGTH - 1) + "😀")

        assertEquals("a".repeat(DeviceName.MAX_LENGTH - 1), cleaned)
    }

    @Test
    fun `an emoji inside the limit stays`() {
        assertEquals("Phone 😀", DeviceName.clean("Phone 😀"))
    }
}

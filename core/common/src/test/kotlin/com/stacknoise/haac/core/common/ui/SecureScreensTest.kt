package com.stacknoise.haac.core.common.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecureScreensTest {
    private val screens = SecureScreens()

    @Test
    fun `the flag is set by the first screen and cleared by the last`() {
        assertTrue(screens.enter())
        assertTrue(screens.leave())
    }

    @Test
    fun `the old screen leaving after the new one entered keeps the flag`() {
        assertTrue(screens.enter())
        assertFalse(screens.enter())
        assertFalse(screens.leave())
        assertTrue(screens.leave())
    }

    @Test
    fun `a leave without an enter changes nothing`() {
        assertFalse(screens.leave())
        assertTrue(screens.enter())
    }
}

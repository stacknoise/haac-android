package com.stacknoise.haac.core.common.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WindowWidthClassTest {
    @Test
    fun `a phone is compact`() {
        assertEquals(WindowWidthClass.Compact, windowWidthClassOf(411))
        assertEquals(WindowWidthClass.Compact, windowWidthClassOf(599))
    }

    @Test
    fun `a tablet in portrait is medium`() {
        assertEquals(WindowWidthClass.Medium, windowWidthClassOf(600))
        assertEquals(WindowWidthClass.Medium, windowWidthClassOf(800))
        assertEquals(WindowWidthClass.Medium, windowWidthClassOf(839))
    }

    @Test
    fun `a tablet in landscape is expanded`() {
        assertEquals(WindowWidthClass.Expanded, windowWidthClassOf(840))
        assertEquals(WindowWidthClass.Expanded, windowWidthClassOf(1280))
    }
}

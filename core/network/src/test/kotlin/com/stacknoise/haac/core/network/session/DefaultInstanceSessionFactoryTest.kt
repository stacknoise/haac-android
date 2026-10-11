package com.stacknoise.haac.core.network.session

import io.mockk.mockk
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class DefaultInstanceSessionFactoryTest {
    private val factory = DefaultInstanceSessionFactory(mockk(), mockk())
    private val home = "https://ha.example.com/".toHttpUrl()

    @Test
    fun `the session of an instance at the same address is reused`() {
        assertSame(factory.create("s1", home), factory.create("s1", home))
    }

    @Test
    fun `another address, another instance or an unverified address gets its own session`() {
        val first = factory.create("s1", home)
        assertNotSame(first, factory.create("s1", "https://other.example.com/".toHttpUrl()))
        assertNotSame(first, factory.create("s2", home))
        assertNotSame(factory.create("s2", home), factory.create("s2", home, verified = false))
    }

    @Test
    fun `discard forgets one session, discardAll every session`() {
        val a = factory.create("a", home)
        val b = factory.create("b", home)
        factory.discard("a")
        assertNotSame(a, factory.create("a", home))
        assertSame(b, factory.create("b", home))
        factory.discardAll()
        assertNotSame(b, factory.create("b", home))
    }
}

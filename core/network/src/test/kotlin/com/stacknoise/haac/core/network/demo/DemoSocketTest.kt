package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.http.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DemoSocketTest {
    private val demo = DemoHarness()
    private val socket = DemoSocket(demo.world, Dispatchers.Unconfined)

    @Test
    fun `a command is answered through receive, including ping`() = runTest {
        assertTrue(socket.send("""{"id":1,"type":"ping"}"""))
        assertEquals("pong", socket.receive().text("type"))
        assertTrue(socket.send("""{"id":2,"type":"haac_bridge/info"}"""))
        val reply = socket.receive()
        assertEquals("2", reply["id"]!!.jsonPrimitive.content)
        assertEquals("true", reply["success"]!!.jsonPrimitive.content)
    }

    @Test
    fun `replies and events arrive in the order they were produced`() = runTest {
        socket.send("""{"id":1,"type":"haac_bridge/subscribe_entities"}""")
        socket.send("""{"id":2,"type":"haac_bridge/call_service",""" + TurnOffSocket.removePrefix("{"))
        val types = List(4) { socket.receive().let { it.text("type") + it["id"]!!.jsonPrimitive.content } }
        assertEquals(listOf("result1", "event1", "result2", "event1"), types)
    }

    @Test
    fun `a frame that is not a JSON object is refused`() {
        assertFalse(socket.send("not json"))
        assertFalse(socket.send("[1]"))
    }

    @Test
    fun `a change made elsewhere reaches a subscription of this socket`() = runTest {
        socket.send("""{"id":1,"type":"haac_bridge/subscribe_entities"}""")
        repeat(2) { socket.receive() }
        demo.request("haac_bridge/call_service", TurnOffSocket)
        assertEquals("event", socket.receive().text("type"))
    }

    @Test
    fun `after close nothing is sent or received and the socket reports a lost connection`() = runTest {
        socket.close()
        assertFalse(socket.send("""{"id":1,"type":"ping"}"""))
        val error = assertThrows<NetworkException> { socket.receive() }
        assertEquals(ErrorCode.NET_UNREACHABLE, error.code)
    }
}

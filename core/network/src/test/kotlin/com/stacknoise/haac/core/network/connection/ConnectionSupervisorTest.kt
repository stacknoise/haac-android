package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.network.bridge.BridgeInfo
import com.stacknoise.haac.core.network.bridge.DefaultBridgeMessageFactory
import com.stacknoise.haac.core.network.websocket.FakeHaWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionSupervisorTest {
    private val internal = "http://192.168.1.10:8123/".toHttpUrl()
    private val external = "https://abc.ui.nabu.casa/".toHttpUrl()
    private val networkChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fake connector: [selected] is the chosen address, [failures] are thrown by the next connects. */
    private inner class FakeConnector : BridgeConnector {
        var selected: HttpUrl = external
        val failures = ArrayDeque<HaacException>()
        val attempts = mutableListOf<HttpUrl>()
        val sockets = mutableListOf<FakeHaWebSocket>()

        override suspend fun select(serverId: String) = selected

        override suspend fun connect(serverId: String, url: HttpUrl, scope: CoroutineScope): BridgeConnection {
            attempts += url
            failures.removeFirstOrNull()?.let { throw it }
            val socket = FakeHaWebSocket.ha().also { sockets += it }
            val info = BridgeInfo("0.1.0", 1, haVersion = "2026.9.0")
            return BridgeConnection(url, info, socket, DefaultBridgeMessageFactory(), DefaultErrorFactory(), scope)
        }
    }

    private val connector = FakeConnector()

    private fun TestScope.supervisor() =
        ConnectionSupervisor(connector, { networkChanges }, { failures -> 1_000L * failures }, backgroundScope)

    @Test
    fun `start connects and publishes the connection`() = runTest {
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        assertEquals(ConnectionState.Connected(external), supervisor.state.value)
        assertEquals(external, supervisor.connection.value?.url)
    }

    @Test
    fun `a lost connection reconnects after the back-off`() = runTest {
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        connector.sockets.single().fail()
        runCurrent()
        val reconnecting = supervisor.state.value as ConnectionState.Reconnecting
        assertEquals(ErrorCode.NET_CONNECTION_LOST, reconnecting.error.code)
        assertNull(supervisor.connection.value)
        advanceTimeBy(1_001)
        assertEquals(ConnectionState.Connected(external), supervisor.state.value)
        assertEquals(2, connector.attempts.size)
    }

    @Test
    fun `failed attempts wait longer each time and retry skips the wait`() = runTest {
        repeat(3) { connector.failures += NetworkException(ErrorCode.NET_UNREACHABLE) }
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        advanceTimeBy(1_001)
        assertEquals(2, connector.attempts.size)
        advanceTimeBy(1_500)
        assertEquals(2, connector.attempts.size)
        advanceTimeBy(501)
        assertEquals(3, connector.attempts.size)
        supervisor.retry()
        runCurrent()
        assertEquals(4, connector.attempts.size)
        assertEquals(ConnectionState.Connected(external), supervisor.state.value)
    }

    @Test
    fun `a wrong server waits for a network change instead of retrying`() = runTest {
        connector.failures += NetworkException(ErrorCode.NET_WRONG_SERVER)
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        assertEquals(ErrorCode.NET_WRONG_SERVER, (supervisor.state.value as ConnectionState.Failed).error.code)
        advanceTimeBy(600_000)
        assertEquals(1, connector.attempts.size)
        networkChanges.emit(Unit)
        runCurrent()
        assertEquals(ConnectionState.Connected(external), supervisor.state.value)
    }

    @Test
    fun `a network change moves the connection to a better address`() = runTest {
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        val first = supervisor.connection.value
        networkChanges.emit(Unit)
        runCurrent()
        assertSame(first, supervisor.connection.value)
        connector.selected = internal
        networkChanges.emit(Unit)
        runCurrent()
        assertTrue(connector.sockets.first().closed)
        assertEquals(listOf(external, internal), connector.attempts)
        assertEquals(ConnectionState.Connected(internal), supervisor.state.value)
    }

    @Test
    fun `stop closes the connection and ends reconnecting`() = runTest {
        val supervisor = supervisor()
        supervisor.start("s1")
        runCurrent()
        supervisor.stop()
        runCurrent()
        assertTrue(connector.sockets.single().closed)
        assertEquals(ConnectionState.Idle, supervisor.state.value)
        advanceTimeBy(600_000)
        assertEquals(1, connector.attempts.size)
    }
}

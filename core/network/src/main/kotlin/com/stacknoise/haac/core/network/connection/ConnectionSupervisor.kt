package com.stacknoise.haac.core.network.connection

import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.di.ConnectionScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl

/** State of the live connection of the active instance (concept 11.4, 14.1). */
sealed interface ConnectionState {
    /** No instance is kept connected, e.g. while the app is in the background. */
    data object Idle : ConnectionState

    /** The first attempt is running. */
    data object Connecting : ConnectionState

    /** Connected at [url]. */
    data class Connected(val url: HttpUrl) : ConnectionState

    /** The last attempt failed or the connection was lost with [error]; the next attempt follows the back-off. */
    data class Reconnecting(val error: HaacException) : ConnectionState

    /** [error] does not pass by itself (e.g. AUTH-003, BRG-001, NET-008); retried on a network change or retry. */
    data class Failed(val error: HaacException) : ConnectionState
}

/**
 * Keeps the active instance connected (concept 4.5, 9.1, 11.4): chooses the address, connects, reconnects with
 * exponential back-off and chooses the address again when the network changes; another address rebuilds the
 * connection. Only one instance is connected at a time (4.4). Runs on the main thread ([ConnectionScope]).
 */
@Singleton
class ConnectionSupervisor @Inject constructor(
    private val connector: BridgeConnector,
    private val network: NetworkMonitor,
    private val backoff: Backoff,
    private val reporter: ErrorReporter,
    @param:ConnectionScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    private val _connection = MutableStateFlow<BridgeConnection?>(null)
    private val wakeUps = Channel<Unit>(Channel.CONFLATED)
    private var serverId: String? = null
    private var job: Job? = null

    /** The current state. */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** The open connection, or null; the sync runs its steps on every new connection (concept 9.1, 11.4). */
    val connection: StateFlow<BridgeConnection?> = _connection.asStateFlow()

    /** Keeps instance [serverId] connected until [stop]; a running supervision of the same instance continues. */
    fun start(serverId: String) {
        if (serverId == this.serverId && job?.isActive == true) return
        stop()
        this.serverId = serverId
        wakeUps.tryReceive()
        _state.value = ConnectionState.Connecting
        job = scope.launch { supervise(serverId) }
    }

    /** Closes the connection and stops reconnecting, e.g. in the background, on lock or sign-out. */
    fun stop() {
        job?.cancel()
        job = null
        serverId = null
        _connection.value?.close()
        _connection.value = null
        _state.value = ConnectionState.Idle
    }

    /** Tries again now instead of waiting for the back-off or a network change. */
    fun retry() {
        wakeUps.trySend(Unit)
    }

    /** Connect, hold, wait, repeat; network changes wake the waiting or held connection. */
    private suspend fun supervise(serverId: String) = coroutineScope {
        launch { network.changes().collect { wakeUps.trySend(Unit) } }
        var failures = 0
        var url: HttpUrl? = null
        while (true) {
            val outcome = try {
                val connection = connector.connect(serverId, url ?: connector.select(serverId), this)
                failures = 0
                hold(serverId, connection)
            } catch (e: HaacException) {
                Outcome.Ended(e)
            }
            url = (outcome as? Outcome.Moved)?.url
            if (outcome is Outcome.Ended) pause(serverId, outcome.reason, ++failures)
        }
    }

    /** Publishes [connection] until it ends or a wake-up finds another address for it. */
    private suspend fun hold(serverId: String, connection: BridgeConnection): Outcome {
        _connection.value = connection
        _state.value = ConnectionState.Connected(connection.url)
        wakeUps.tryReceive()
        try {
            while (true) {
                val reason = select {
                    connection.end.onAwait { it }
                    wakeUps.onReceive { null }
                }
                if (reason != null) return Outcome.Ended(reason)
                reselect(serverId, connection)?.let { return Outcome.Moved(it) }
            }
        } finally {
            connection.close()
            if (_connection.value === connection) _connection.value = null
        }
    }

    /** The address to move to, or null to stay; a failed selection keeps the working connection. */
    private suspend fun reselect(serverId: String, connection: BridgeConnection): HttpUrl? = try {
        connector.select(serverId).takeIf { it != connection.url }
    } catch (_: HaacException) {
        null
    }

    /**
     * Reports [reason] to the notification list (concept 14.1, 17.4) and waits after [failures] failures: the
     * back-off, or for a network change or [retry] if waiting cannot help.
     */
    private suspend fun pause(serverId: String, reason: HaacException, failures: Int) {
        reporter.report(reason, serverId)
        if (reason.code in NEEDS_CHANGE) {
            _state.value = ConnectionState.Failed(reason)
            wakeUps.receive()
        } else {
            _state.value = ConnectionState.Reconnecting(reason)
            withTimeoutOrNull(backoff.delayMs(failures)) { wakeUps.receive() }
        }
    }

    /** How one connection ended. */
    private sealed interface Outcome {
        /** Lost or failed with [reason]. */
        data class Ended(val reason: HaacException) : Outcome

        /** Closed because [url] is the better address now (concept 4.5). */
        data class Moved(val url: HttpUrl) : Outcome
    }

    /** Errors that retrying at the same place cannot fix. */
    private companion object {
        val NEEDS_CHANGE = setOf(
            ErrorCode.AUTH_SESSION_EXPIRED,
            ErrorCode.AUTH_USER_BLOCKED,
            ErrorCode.SEC_BIOMETRICS_CHANGED,
            ErrorCode.SEC_STORAGE_UNAVAILABLE,
            ErrorCode.SEC_LOCKED,
            ErrorCode.BRG_NOT_INSTALLED,
            ErrorCode.BRG_UPDATE_REQUIRED,
            ErrorCode.NET_WRONG_SERVER,
            ErrorCode.NET_CERTIFICATE_CHANGED,
            ErrorCode.NET_CERTIFICATE_UNTRUSTED,
            ErrorCode.NET_CLEARTEXT_NOT_ALLOWED,
            ErrorCode.NET_INVALID_ADDRESS,
        )
    }
}

package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.ErrorFactory
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.error.NetworkException
import com.stacknoise.haac.core.error.ValidationException
import com.stacknoise.haac.core.error.database
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.core.network.di.ConnectionScope
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.ServiceCallFactory
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/**
 * Carries out control requests (concept 8.1, 8.4, 11.2): shows the expected state at once ([PendingStates]),
 * sends `haac_bridge/call_service` and keeps the expected state until HA confirms it; a failed call or a
 * different confirmed state rolls it back. A newer request of the same entity replaces the older one; target
 * temperatures are debounced so tapping + several times sends one call.
 */
@Singleton
class EntityController @Inject constructor(
    private val live: LiveConnection,
    private val entities: ExposedEntityDao,
    private val tools: ControlTools,
    private val pending: PendingStates,
    private val failures: ControlFailures,
    @param:ConnectionScope private val scope: CoroutineScope,
) {
    private val jobs = ConcurrentHashMap<EntityKey, Job>()

    /** True while a connection is open; controls are disabled otherwise (concept 14.1). */
    val connected: Flow<Boolean> = live.connection.map { it != null }.distinctUntilChanged()

    /** Failed calls for the snackbar (concept 14.1). */
    val failed: SharedFlow<HaacException> = failures.failures

    /** Sends [request] for [entityId] of instance [serverId]; see the class description. */
    fun send(serverId: String, entityId: String, request: ControlRequest) {
        val key = EntityKey(serverId, entityId)
        pending.set(key, request)
        jobs.remove(key)?.cancel()
        // Started after it is stored, so a call that ends at once still removes itself from [jobs].
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (request is ControlRequest.SetTemperature) delay(DEBOUNCE_MS)
                perform(key, request)
            } catch (e: HaacException) {
                failures.handle(serverId, e)
            } finally {
                pending.clear(key, request)
                jobs.remove(key, coroutineContext[Job])
            }
        }
        jobs[key] = job
        job.start()
    }

    /** Sends the call, then waits up to [CONFIRM_MS] for HA's state to show the result. */
    private suspend fun perform(key: EntityKey, request: ControlRequest) {
        val entity = tools.errors.database { entities.all(key.serverId) }.firstOrNull { it.entityId == key.entityId }
            ?: throw ValidationException(ErrorCode.ENT_NOT_FOUND)
        val channel = live.connection.value?.takeIf { it.isOpen }
            ?: throw NetworkException(ErrorCode.NET_CONNECTION_LOST)
        channel.request(CALL_SERVICE, tools.calls.create(entity, request).fields())
        withTimeoutOrNull(CONFIRM_MS) {
            entities.observe(key.serverId)
                .map { rows -> rows.firstOrNull { it.entityId == key.entityId }?.lastState }
                .first { tools.json.decodeState(it)?.let(request::isConfirmedBy) == true }
        }
    }

    /** Timing (concept 8.4) and the command name (11.2). */
    internal companion object {
        /** Wait after the last temperature step before the call is sent. */
        const val DEBOUNCE_MS = 800L

        /** How long the expected state is shown without confirmation before HA's state shows again. */
        const val CONFIRM_MS = 5_000L

        /** The service call command. */
        const val CALL_SERVICE = "haac_bridge/call_service"
    }
}

/** What [EntityController] needs to build calls and read states. */
class ControlTools @Inject constructor(
    /** Builds and checks the service calls. */
    val calls: ServiceCallFactory,
    /** Converts database errors. */
    val errors: ErrorFactory,
    /** Decodes the cached states. */
    val json: Json,
)

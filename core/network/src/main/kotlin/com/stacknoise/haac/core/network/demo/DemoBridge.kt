package com.stacknoise.haac.core.network.demo

import com.stacknoise.haac.core.network.http.text
import com.stacknoise.haac.core.network.websocket.messageId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * The bridge of the demo instance (concept 20.2): answers the bridge commands (`haac_bridge/...`) of chapters 11
 * and 19.4 from a [DemoWorld] with the same JSON as HAAC Bridge, including error replies with HAB codes, and
 * sends the events of the subscriptions. Every reply and event goes to [emit]; [handle] runs one command at a time.
 */
class DemoBridge(private val world: DemoWorld, private val emit: (JsonObject) -> Unit) : DemoWorldListener {
    private val schedules = DemoSchedules(world.clock)
    private val queries = DemoQueryCommands(world, DemoHistory(world.clock), emit)
    private val scheduleCommands = DemoScheduleCommands(world, schedules, emit)
    private val entitySubscriptions = ConcurrentHashMap.newKeySet<Int>()
    private val scheduleSubscriptions = ConcurrentHashMap.newKeySet<Int>()

    init {
        world.addListener(this)
    }

    /** Answers one command of the app; a message without an `id` (such as `auth`) is ignored. */
    fun handle(message: JsonObject) {
        val id = message.messageId ?: return
        world.locked { dispatch(id, message.text("type").orEmpty(), message) }
    }

    /** Stops listening to the world, e.g. when the socket closes. */
    fun close() {
        world.removeListener(this)
    }

    /** The changed entities go to every state subscription: new ones as `a`, changed ones as `c` (concept 11.3). */
    override fun entitiesChanged(changes: List<EntityChange>) {
        val added = changes.filter { it.before == null }
            .associate { it.after.entityId to DemoEntityWire.compressed(it.after) }
        val changed = changes.mapNotNull { change ->
            change.before?.takeIf { it != change.after }
                ?.let { change.after.entityId to DemoEntityWire.diff(it, change.after) }
        }.toMap()
        if (added.isEmpty() && changed.isEmpty()) return
        val payload = buildJsonObject {
            if (added.isNotEmpty()) put("a", JsonObject(added))
            if (changed.isNotEmpty()) put("c", JsonObject(changed))
        }
        entitySubscriptions.forEach { emit(DemoReplies.event(it, payload)) }
    }

    /** Every schedule subscription gets `schedules_changed` with the new revision. */
    override fun schedulesChanged() {
        val payload = buildJsonObject { put("schedules_changed", scheduleCommands.revision()) }
        scheduleSubscriptions.forEach { emit(DemoReplies.event(it, payload)) }
    }

    /** Routes the command [type] to its handler; an unknown one is `unknown_command`, as in HA. */
    private fun dispatch(id: Int, type: String, message: JsonObject) {
        when (type) {
            "ping" -> emit(DemoReplies.pong(id))
            "haac_bridge/subscribe_entities" -> subscribeEntities(id)
            "haac_bridge/subscribe_schedules" -> {
                scheduleSubscriptions += id
                emit(DemoReplies.result(id))
            }
            "unsubscribe_events" -> unsubscribe(id, message)
            "haac_bridge/call_service" -> callService(id, message)
            else -> if (!queries.handle(type, id, message) && !scheduleCommands.handle(type, id, message)) {
                emit(DemoReplies.error(id, "unknown_command", "Unknown command."))
            }
        }
    }

    /** Registers the subscription, answers with an empty result and sends the states of all entities as `a`. */
    private fun subscribeEntities(id: Int) {
        entitySubscriptions += id
        emit(DemoReplies.result(id))
        val all = world.snapshot().entities.associate { it.entityId to DemoEntityWire.compressed(it) }
        emit(DemoReplies.event(id, buildJsonObject { put("a", JsonObject(all)) }))
    }

    /** Ends the subscription named by `subscription`. */
    private fun unsubscribe(id: Int, message: JsonObject) {
        val subscription = (message["subscription"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        if (subscription != null) {
            entitySubscriptions -= subscription
            scheduleSubscriptions -= subscription
        }
        emit(DemoReplies.result(id))
    }

    /**
     * `call_service`: HAB-WS-001 for missing fields or target keys in `service_data`, HAB-ENT-001 for an unknown
     * entity, HAB-SVC-002 or -003 from [DemoServices]; otherwise the empty result, then the state event.
     */
    private fun callService(id: Int, message: JsonObject) {
        val entityId = message.text("entity_id")
        val service = message.text("service")
        val data = message["service_data"] as? JsonObject ?: JsonObject(emptyMap())
        if (entityId == null || service == null || data.keys.any { it in TARGET_KEYS }) {
            emit(DemoReplies.error(id, DemoCodes.WS_INVALID_REQUEST, "Invalid service call"))
            return
        }
        val entity = world.snapshot().entities.firstOrNull { it.entityId == entityId }
        if (entity == null) {
            emit(DemoReplies.error(id, DemoCodes.ENT_NOT_FOUND, "$entityId does not exist"))
            return
        }
        when (val outcome = DemoServices.call(entity, service, data, world.clock.millis())) {
            is DemoOutcome.Failed -> emit(DemoReplies.failure(id, outcome))
            is DemoOutcome.Ok -> {
                emit(DemoReplies.result(id))
                if (outcome.value != entity) world.setEntities(listOf(outcome.value))
            }
        }
    }

    /** Keys the bridge never accepts in `service_data`, because it sets the target itself (concept 11.4). */
    private companion object {
        val TARGET_KEYS = setOf("entity_id", "device_id", "area_id", "floor_id", "label_id")
    }
}

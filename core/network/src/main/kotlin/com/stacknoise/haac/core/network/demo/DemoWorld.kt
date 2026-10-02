package com.stacknoise.haac.core.network.demo

import java.time.Clock
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** An entity before and after a change; [before] is null if the entity is new. */
class EntityChange(val before: DemoEntity?, val after: DemoEntity)

/** Hears about changes of the demo world, e.g. a [DemoBridge] that sends them to its subscriptions. */
interface DemoWorldListener {
    /** The entities in [changes] have a new state or new attributes. */
    fun entitiesChanged(changes: List<EntityChange>)

    /** A schedule was created, changed, deleted or run. */
    fun schedulesChanged()
}

/**
 * The state of the demo bridge in memory (concept 20.2, 20.5): entities, floors, areas and schedules. It is read
 * from the store when first needed (a missing or damaged file gives the default data) and written after every
 * change. All changes happen under [locked], so a command and the events it causes are not interleaved.
 */
@Singleton
class DemoWorld internal constructor(
    private val store: DemoWorldStore,
    private val json: Json,
    /** The clock of the demo; its zone is the time zone of the phone (concept 20.3). */
    val clock: Clock,
) {
    /** Uses the system clock and its time zone. */
    @Inject
    constructor(store: DemoWorldStore, json: Json) : this(store, json, Clock.systemDefaultZone())

    private val lock = Any()
    private val listeners = CopyOnWriteArraySet<DemoWorldListener>()
    private var data: DemoWorldData? = null

    /** Runs [block] with the world locked. */
    fun <T> locked(block: () -> T): T = synchronized(lock, block)

    /** The current data. */
    fun snapshot(): DemoWorldData = locked { data ?: read().also { data = it } }

    /** Replaces the entities with the IDs of [updated], saves and tells the listeners. */
    fun setEntities(updated: List<DemoEntity>) = locked {
        val current = snapshot()
        val before = current.entities.associateBy { it.entityId }
        val byId = updated.associateBy { it.entityId }
        save(current.copy(entities = current.entities.map { byId[it.entityId] ?: it }))
        val changes = updated.map { EntityChange(before[it.entityId], it) }
        listeners.forEach { it.entitiesChanged(changes) }
    }

    /** Replaces all schedules, saves and tells the listeners. */
    fun setSchedules(schedules: List<DemoSchedule>) = locked {
        save(snapshot().copy(schedules = schedules))
        listeners.forEach { it.schedulesChanged() }
    }

    /** Deletes the saved state; the next access starts with the default data (concept 20.4). */
    fun discard() = locked {
        store.delete()
        data = null
    }

    /** Registers [listener] until [removeListener]. */
    fun addListener(listener: DemoWorldListener) {
        listeners += listener
    }

    /** Stops telling [listener] about changes. */
    fun removeListener(listener: DemoWorldListener) {
        listeners -= listener
    }

    /** Keeps [new] as the data and writes it to the store. */
    private fun save(new: DemoWorldData) {
        data = new
        store.save(json.encodeToString(DemoWorldData.serializer(), new))
    }

    /** The saved data, or the default data (saved at once, so timestamps stay) if there is none or it is damaged. */
    private fun read(): DemoWorldData {
        val saved = store.load()?.let {
            try {
                json.decodeFromString(DemoWorldData.serializer(), it)
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return saved ?: DemoWorldData.initial(clock.millis(), DemoTimes.iso(clock.millis())).also(::save)
    }
}

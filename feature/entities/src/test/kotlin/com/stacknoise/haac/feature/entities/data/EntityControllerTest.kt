package com.stacknoise.haac.feature.entities.data

import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.error.BridgeException
import com.stacknoise.haac.core.error.DefaultErrorFactory
import com.stacknoise.haac.core.error.ErrorCode
import com.stacknoise.haac.core.error.HaacException
import com.stacknoise.haac.core.network.connection.BridgeChannel
import com.stacknoise.haac.core.network.connection.LiveConnection
import com.stacknoise.haac.feature.entities.domain.ControlRequest
import com.stacknoise.haac.feature.entities.domain.DefaultServiceCallFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [EntityController]: optimistic state, confirmation, rollback and debounce (concept 8.1, 8.4, 14.1). */
@OptIn(ExperimentalCoroutinesApi::class)
class EntityControllerTest {
    private val table = MutableStateFlow(
        listOf(
            ExposedEntity("s1", "switch.lamp", "switch", "Lamp", lastState = """{"state":"off"}"""),
            ExposedEntity(
                "s1",
                "climate.radiator",
                "climate",
                "Radiator",
                supportedFeatures = 1,
                lastState = """{"state":"heat","attributes":{"temperature":21.0}}""",
            ),
        ),
    )

    /** Reads come from [table]; the sync methods are not used by the controller. */
    private val dao = object : ExposedEntityDao {
        override fun observe(serverId: String): Flow<List<ExposedEntity>> = table

        override suspend fun all(serverId: String): List<ExposedEntity> = table.value

        override suspend fun state(serverId: String, entityId: String): String? = null

        override suspend fun setState(serverId: String, entityId: String, state: String) = Unit

        override suspend fun upsert(rows: List<ExposedEntity>) = Unit

        override suspend fun markSynced(serverId: String, revision: String, at: Long) = Unit

        override suspend fun revision(serverId: String): String? = null
    }

    /** Records the service calls; [failure] rejects them. */
    private val channel = object : BridgeChannel {
        override val isOpen = true
        val calls = mutableListOf<JsonObject>()
        var failure: HaacException? = null

        override suspend fun request(type: String, fields: JsonObject): JsonElement {
            assertEquals(EntityController.CALL_SERVICE, type)
            calls += fields
            failure?.let { throw it }
            return JsonNull
        }

        override fun subscribe(type: String, fields: JsonObject): Flow<JsonObject> = emptyFlow()
    }

    private val live = object : LiveConnection {
        override val connection = MutableStateFlow<BridgeChannel?>(channel)
    }

    private val pending = PendingStates()
    private val reported = mutableListOf<ErrorCode>()
    private var rechecks = 0
    private val failures = ControlFailures({ error, _ -> reported += error.code }) { rechecks++ }
    private val tools = ControlTools(DefaultServiceCallFactory(), DefaultErrorFactory(), Json)

    private fun TestScope.controller() = EntityController(live, dao, tools, pending, failures, backgroundScope)

    private suspend fun shown(entityId: String): ControlRequest? = pending.of("s1").first()[entityId]

    /** Writes [state] of [entityId] as the subscription would. */
    private fun confirm(entityId: String, state: String) {
        table.value = table.value.map { if (it.entityId == entityId) it.copy(lastState = state) else it }
    }

    @Test
    fun `a toggle shows at once and stays until HA confirms it`() = runTest {
        controller().send("s1", "switch.lamp", ControlRequest.SwitchTo(on = true))
        assertEquals(ControlRequest.SwitchTo(on = true), shown("switch.lamp"))
        runCurrent()
        assertEquals("turn_on", channel.calls.single()["service"]?.jsonPrimitive?.content)
        assertEquals(ControlRequest.SwitchTo(on = true), shown("switch.lamp"))
        confirm("switch.lamp", """{"state":"on"}""")
        runCurrent()
        assertEquals(null, shown("switch.lamp"))
    }

    @Test
    fun `a rejected call rolls back, reports and checks the revision`() = runTest {
        channel.failure = BridgeException(ErrorCode.BRG_NOT_ALLOWED, bridgeCode = "HAB-SVC-001")
        val controller = controller()
        controller.send("s1", "switch.lamp", ControlRequest.SwitchTo(on = true))
        runCurrent()
        assertEquals(null, shown("switch.lamp"))
        assertEquals(listOf(ErrorCode.BRG_NOT_ALLOWED), reported)
        assertEquals(1, rechecks)
    }

    @Test
    fun `temperature steps are debounced into one call`() = runTest {
        val controller = controller()
        controller.send("s1", "climate.radiator", ControlRequest.SetTemperature(21.5))
        advanceTimeBy(500)
        controller.send("s1", "climate.radiator", ControlRequest.SetTemperature(22.0))
        advanceTimeBy(700)
        assertTrue(channel.calls.isEmpty())
        assertEquals(ControlRequest.SetTemperature(22.0), shown("climate.radiator"))
        advanceTimeBy(200)
        assertEquals("""{"temperature":22.0}""", channel.calls.single()["service_data"].toString())
    }

    @Test
    fun `without confirmation HA's state shows again after the timeout`() = runTest {
        controller().send("s1", "switch.lamp", ControlRequest.SwitchTo(on = true))
        advanceTimeBy(EntityController.CONFIRM_MS - 1)
        assertEquals(ControlRequest.SwitchTo(on = true), shown("switch.lamp"))
        advanceTimeBy(2)
        assertEquals(null, shown("switch.lamp"))
        assertTrue(reported.isEmpty())
    }

    @Test
    fun `without a connection the call fails with HAAC-NET-002`() = runTest {
        live.connection.value = null
        controller().send("s1", "switch.lamp", ControlRequest.SwitchTo(on = true))
        runCurrent()
        assertEquals(listOf(ErrorCode.NET_CONNECTION_LOST), reported)
        assertEquals(0, rechecks)
        assertEquals(null, shown("switch.lamp"))
    }
}

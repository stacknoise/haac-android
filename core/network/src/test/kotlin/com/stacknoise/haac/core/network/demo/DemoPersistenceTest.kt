package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DemoPersistenceTest {
    private val store = MemoryDemoWorldStore()

    private fun states(demo: DemoHarness) = demo.list(demo.ask("haac_bridge/entities/list"), "entities")
        .associate { it.str("entity_id") to it.str("state") }

    @Test
    fun `the default data is saved as soon as the demo connects`() {
        assertNull(store.saved)
        DemoHarness(store).ask("haac_bridge/info")
        DemoHarness(store).world.snapshot()
        assertNotNull(store.saved)
    }

    @Test
    fun `a change is written and a new world reads it, so a restart keeps states and schedules`() {
        val first = DemoHarness(store)
        first.request("haac_bridge/call_service", TurnOffSocket)
        first.request(
            "haac_bridge/schedules/create",
            """{"name":"Kept","when":{"type":"time","time":"21:00","days":[1]},""" +
                """"action":"turn_on","entities":["switch.demo_socket"]}""",
        )
        val second = DemoHarness(store)
        assertEquals("off", states(second)["switch.demo_socket"])
        val names = second.list(second.ask("haac_bridge/schedules/list"), "schedules")
            .map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(listOf("Morning light", "Kept"), names)
    }

    @Test
    fun `revisions are the same after a restart`() {
        val first = DemoHarness(store)
        val entities = first.ask("haac_bridge/exposure/revision")["revision"]
        val schedules = first.ask("haac_bridge/schedules/revision")["revision"]
        val second = DemoHarness(store)
        assertEquals(entities, second.ask("haac_bridge/exposure/revision")["revision"])
        assertEquals(schedules, second.ask("haac_bridge/schedules/revision")["revision"])
    }

    @Test
    fun `a damaged file leads to the default data`() {
        store.save("{ this is not json")
        val demo = DemoHarness(store)
        assertEquals(7, states(demo).size)
        assertEquals("on", states(demo)["switch.demo_socket"])
    }

    @Test
    fun `a file with the wrong structure leads to the default data`() {
        store.save("""{"entities":"none"}""")
        assertEquals(7, states(DemoHarness(store)).size)
    }

    @Test
    fun `discard deletes the file and the next access starts again with the default data`() {
        val demo = DemoHarness(store)
        demo.request("haac_bridge/call_service", TurnOffSocket)
        demo.world.discard()
        assertNull(store.saved)
        assertEquals("on", states(demo)["switch.demo_socket"])
    }
}

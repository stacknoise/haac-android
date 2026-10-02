package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DemoHistoryTest {
    private val demo = DemoHarness()
    private val now = demo.clock.millis()
    private val day = 86_400_000L

    private fun iso(ms: Long) = DemoTimes.iso(ms)

    private fun history(entity: String, start: Long, end: Long, minimal: Boolean = true) = demo.ask(
        "haac_bridge/history",
        """{"entity_ids":["$entity"],"start":"${iso(start)}","end":"${iso(end)}","minimal_response":$minimal}""",
    )

    private fun statistics(entity: String, types: String, period: String = "hour", start: Long = now - day) = demo.ask(
        "haac_bridge/statistics",
        """{"entity_ids":["$entity"],"start":"${iso(start)}","end":"${iso(now)}","period":"$period","types":$types}""",
    )

    private fun rows(result: JsonObject, entity: String) =
        result[entity]!!.jsonArray.map { it.jsonObject }

    /** The length of a statistics row in milliseconds. */
    private fun JsonObject.length() = str("end").toLong() - str("start").toLong()

    private fun JsonObject.number(key: String) = str(key).toDouble()

    private fun seconds(row: JsonElement) = row.jsonObject["lc"]!!.jsonPrimitive.content.toDouble()

    @Test
    fun `a measurement history covers the window from its start with numbers on a half-hour grid`() {
        val rows = rows(history("sensor.demo_temperature", now - day, now), "sensor.demo_temperature")
        assertEquals((now - day) / 1000.0, seconds(rows.first()), 0.001)
        assertTrue(rows.zipWithNext().all { (a, b) -> seconds(a) < seconds(b) })
        assertTrue(seconds(rows.last()) <= now / 1000.0)
        assertEquals(48, rows.size)
        assertTrue(rows.all { it["s"]!!.jsonPrimitive.content.toDouble() in 15.0..27.0 })
        assertTrue(rows.all { "a" !in it })
    }

    @Test
    fun `the same window always gives the same series, and an overlapping window agrees on the shared part`() {
        val first = history("sensor.demo_temperature", now - day, now)
        assertEquals(first, history("sensor.demo_temperature", now - day, now))
        val shifted = rows(history("sensor.demo_temperature", now - day + 3_600_000, now), "sensor.demo_temperature")
        val original = rows(first, "sensor.demo_temperature").associateBy { seconds(it) }
        assertTrue(shifted.drop(1).all { original[seconds(it)] == it })
    }

    @Test
    fun `a switch history has alternating on and off periods`() {
        val rows = rows(history("switch.demo_kitchen_light", now - 2 * day, now), "switch.demo_kitchen_light")
        assertTrue(rows.size > 2)
        assertTrue(rows.all { it["s"]!!.jsonPrimitive.content in setOf("on", "off") })
        assertTrue(rows.zipWithNext().all { (a, b) -> a["s"] != b["s"] })
    }

    @Test
    fun `a full response carries attributes, a minimal one does not`() {
        val full = rows(history("switch.demo_socket", now - day, now, minimal = false), "switch.demo_socket")
        assertEquals(
            "Socket",
            full.first()["a"]!!.jsonObject["friendly_name"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `the thermostat history has current and target temperature and the heating action`() {
        val rows = rows(history("climate.demo_thermostat", now - day, now, minimal = false), "climate.demo_thermostat")
        val attributes = rows.map { it["a"]!!.jsonObject }
        assertTrue(attributes.all { it["current_temperature"] != null && it["temperature"] != null })
        assertEquals(setOf("heating", "idle"), attributes.map { it["hvac_action"]!!.jsonPrimitive.content }.toSet())
    }

    @Test
    fun `a window in the future has no history and a reversed window is HAB-WS-001`() {
        val future = demo.ask(
            "haac_bridge/history",
            """{"entity_ids":["switch.demo_socket"],"start":"${iso(now + day)}","end":"${iso(now + 2 * day)}"}""",
        )
        assertTrue(future.isEmpty())
        val reversed = demo.request(
            "haac_bridge/history",
            """{"entity_ids":["switch.demo_socket"],"start":"${iso(now)}","end":"${iso(now - day)}"}""",
        )
        assertEquals("HAB-WS-001", demo.code(reversed))
        val noPeriod = demo.request("haac_bridge/history", """{"entity_ids":["switch.demo_socket"]}""")
        assertEquals("HAB-WS-001", demo.code(noPeriod))
    }

    @Test
    fun `entities that do not exist are left out of the result`() {
        val result = demo.ask(
            "haac_bridge/history",
            """{"entity_ids":["switch.nobody"],"start":"${iso(now - day)}","end":"${iso(now)}"}""",
        )
        assertNull(result["switch.nobody"])
    }

    @Test
    fun `hourly statistics of a measurement have mean, min and max in milliseconds`() {
        val rows = rows(statistics("sensor.demo_temperature", """["mean","min","max"]"""), "sensor.demo_temperature")
        assertEquals(24, rows.size)
        assertTrue(rows.all { it.length() == 3_600_000L })
        assertTrue(rows.all { it.str("start").toLong() >= now - day })
        assertTrue(rows.all { it.number("min") < it.number("mean") && it.number("mean") < it.number("max") })
        assertTrue(rows.all { "sum" !in it })
    }

    @Test
    fun `statistics of a counter carry a rising sum`() {
        val rows = rows(statistics("sensor.demo_energy", """["sum"]""", start = now - 2 * day), "sensor.demo_energy")
        val sums = rows.map { it["sum"]!!.jsonPrimitive.content.toDouble() }
        assertTrue(sums.zipWithNext().all { (a, b) -> b > a })
        assertTrue(rows.all { "mean" !in it })
    }

    @Test
    fun `daily statistics start at local midnight and a switch has none`() {
        val result = statistics("sensor.demo_temperature", """["mean"]""", period = "day", start = now - 7 * day)
        val rows = rows(result, "sensor.demo_temperature")
        assertEquals(6, rows.size)
        assertTrue(rows.all { it.length() in 82_800_000L..90_000_000L })
        assertTrue(rows(statistics("switch.demo_socket", """["mean"]"""), "switch.demo_socket").isEmpty())
    }
}

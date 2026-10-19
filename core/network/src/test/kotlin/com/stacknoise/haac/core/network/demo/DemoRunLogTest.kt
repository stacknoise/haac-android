package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DemoRunLogTest {
    private val demo = DemoHarness()
    private val morning = "demo-morning-light"

    private fun runs(fields: String = "{}") = demo.ask("haac_bridge/schedules/runs", fields)

    private fun runNow() = demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"$morning"}""")

    @Test
    fun `a new log is empty and has an id`() {
        val log = runs()
        assertEquals(0, log.str("last_seq").toInt())
        assertEquals(0, log["entries"]!!.jsonArray.size)
        assertEquals(log.str("log_id"), runs().str("log_id"))
    }

    @Test
    fun `run now writes a manual entry with the schedule name and notify`() {
        runNow()
        val log = runs()
        assertEquals("1", log.str("last_seq"))
        val entry = log["entries"]!!.jsonArray.single().jsonObject
        assertEquals(morning, entry.str("schedule_id"))
        assertEquals("Morning light", entry.str("name"))
        assertEquals("ok", entry.str("result"))
        assertEquals("manual", entry.str("trigger"))
        assertEquals("false", entry.str("removed"))
        assertEquals("true", entry.str("notify"))
    }

    @Test
    fun `entries after the cursor of the same log are returned, a different log id returns all`() {
        runNow()
        val logId = runs().str("log_id")
        runNow()
        val after = runs("""{"log_id":"$logId","after_seq":1}""")
        assertEquals(listOf("2"), after["entries"]!!.jsonArray.map { it.str("seq") })
        assertEquals("2", after.str("last_seq"))
        val other = runs("""{"log_id":"other","after_seq":2}""")
        assertEquals(2, other["entries"]!!.jsonArray.size)
    }

    @Test
    fun `changing a schedule without running it adds no entry`() {
        val before = demo.ask("haac_bridge/schedules/list")["schedules"]!!.jsonArray.single().jsonObject
        demo.request(
            "haac_bridge/schedules/update",
            """{"schedule_id":"$morning","updated_at":"${before.str("updated_at")}","enabled":false}""",
        )
        assertEquals(0, runs()["entries"]!!.jsonArray.size)
    }
}

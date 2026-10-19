package com.stacknoise.haac.core.network.demo

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The repeat rules of the demo bridge (concept 19.10); the harness clock stands on Friday 2026-10-02 12:00 Berlin. */
class DemoRepeatTest {
    private val demo = DemoHarness()

    private fun repeat(rule: String, days: String = "[0]", time: String = "06:30") =
        """{"name":"Rule","when":{"type":"time","time":"$time","days":$days},"action":"turn_on",""" +
            """"entities":["switch.demo_socket"],"repeat":$rule}"""

    private fun create(fields: String) = demo.request("haac_bridge/schedules/create", fields)

    /** The `next_runs` of [reply] as `yyyy-MM-dd HH:mm`. */
    private fun runs(reply: JsonObject) =
        reply["next_runs"]!!.jsonArray.map { it.jsonPrimitive.content }.map { it.take(10) + " " + it.substring(11, 16) }

    private fun runsOf(fields: String) = runs(demo.result(create(fields)).jsonObject)

    @Test
    fun `a schedule without a repeat rule is weekly from today and has three next runs`() {
        val fields = """{"name":"A","when":{"type":"time","time":"21:00","days":[0,1,2,3,4,5,6]},"action":"toggle",""" +
            """"entities":["switch.demo_socket"]}"""
        val created = demo.result(create(fields)).jsonObject
        val rule = created.obj("repeat")
        assertEquals("weekly", rule.str("freq"))
        assertEquals("1", rule.str("interval"))
        assertEquals("2026-10-02", rule.str("start_date"))
        assertEquals(listOf("2026-10-02 21:00", "2026-10-03 21:00", "2026-10-04 21:00"), runs(created))
    }

    @Test
    fun `every second week on Monday and Thursday counts the weeks from the start date`() {
        val rule = """{"freq":"weekly","interval":2,"start_date":"2026-10-05"}"""
        assertEquals(
            listOf("2026-10-05 06:30", "2026-10-08 06:30", "2026-10-19 06:30"),
            runsOf(repeat(rule, days = "[0,3]")),
        )
    }

    @Test
    fun `daily every third day and monthly on the 31st use the last day of a short month`() {
        val daily = """{"freq":"daily","interval":3,"start_date":"2026-10-01"}"""
        assertEquals(listOf("2026-10-04 06:30", "2026-10-07 06:30", "2026-10-10 06:30"), runsOf(repeat(daily)))
        val monthly = """{"freq":"monthly","interval":1,"start_date":"2026-10-31"}"""
        assertEquals(listOf("2026-10-31 06:30", "2026-11-30 06:30", "2026-12-31 06:30"), runsOf(repeat(monthly)))
    }

    @Test
    fun `yearly on 29 February runs on 28 February in other years`() {
        val yearly = """{"freq":"yearly","interval":1,"start_date":"2028-02-29"}"""
        assertEquals(listOf("2028-02-29 06:30", "2029-02-28 06:30", "2030-02-28 06:30"), runsOf(repeat(yearly)))
    }

    @Test
    fun `an end date ends the runs and a one-off schedule runs exactly once`() {
        val ending = """{"freq":"daily","interval":1,"start_date":"2026-10-03","end_date":"2026-10-04"}"""
        assertEquals(listOf("2026-10-03 06:30", "2026-10-04 06:30"), runsOf(repeat(ending)))
        val once = demo.result(create(repeat("""{"freq":"once","start_date":"2026-10-05"}"""))).jsonObject
        assertEquals(listOf("2026-10-05 06:30"), runs(once))
        assertEquals("false", once.str("expired"))
        assertTrue(once.obj("repeat")["interval"] == null)
    }

    @Test
    fun `fields that break the rules are HAB-SCH-001`() {
        val invalid = listOf(
            repeat("""{"freq":"fortnightly"}"""),
            repeat("""{"freq":"weekly","interval":53}"""),
            repeat("""{"freq":"yearly","interval":2}"""),
            repeat("""{"freq":"once"}"""),
            repeat("""{"freq":"once","start_date":"2026-10-05","interval":1}"""),
            repeat("""{"freq":"once","start_date":"2026-10-05","end_date":"2026-10-06"}"""),
            repeat("""{"freq":"once","start_date":"2027-10-04"}"""),
            repeat("""{"freq":"daily","start_date":"2026-10-05","end_date":"2026-10-04"}"""),
            repeat("""{"freq":"daily","start_date":"05.10.2026"}"""),
            repeat("""{"freq":"weekly","interval":1}""", days = "[]"),
        )
        invalid.forEach { assertEquals("HAB-SCH-001", demo.code(create(it)), it) }
    }

    @Test
    fun `dates and times in the past are HAB-SCH-009`() {
        val past = listOf(
            repeat("""{"freq":"once","start_date":"2026-10-01"}"""),
            repeat("""{"freq":"once","start_date":"2026-10-02"}""", time = "11:00"),
            repeat("""{"freq":"daily","start_date":"2026-09-01","end_date":"2026-10-01"}"""),
        )
        past.forEach { assertEquals("HAB-SCH-009", demo.code(create(it)), it) }
        assertEquals(1, demo.list(demo.ask("haac_bridge/schedules/list"), "schedules").size)
    }

    @Test
    fun `daily needs no weekdays and notify is kept`() {
        val fields = """{"name":"N","notify":true,"when":{"type":"time","time":"06:30"},"action":"turn_on",""" +
            """"entities":["switch.demo_socket"],"repeat":{"freq":"daily","interval":1}}"""
        val created = demo.result(create(fields)).jsonObject
        assertEquals("true", created.str("notify"))
        assertEquals(listOf("2026-10-03 06:30", "2026-10-04 06:30", "2026-10-05 06:30"), runs(created))
    }

    @Test
    fun `preview answers the next runs of a draft and the same errors as create`() {
        val rule = """{"freq":"once","start_date":"2026-10-05"}"""
        val reply = demo.result(demo.request("haac_bridge/schedules/preview", preview(rule))).jsonObject
        assertEquals(listOf("2026-10-05 06:30"), runs(reply))
        val yesterday = preview("""{"freq":"once","start_date":"2026-10-01"}""")
        val past = demo.request("haac_bridge/schedules/preview", yesterday)
        assertEquals("HAB-SCH-009", demo.code(past))
        val unknown = demo.request("haac_bridge/schedules/preview", preview("""{"freq":"nope"}"""))
        assertEquals("HAB-SCH-001", demo.code(unknown))
        assertEquals(1, demo.list(demo.ask("haac_bridge/schedules/list"), "schedules").size)
    }

    private fun preview(rule: String) = """{"when":{"type":"time","time":"06:30"},"repeat":$rule}"""

    @Test
    fun `run now removes a one-off schedule and keeps a repeating one`() {
        val once = demo.result(create(repeat("""{"freq":"once","start_date":"2026-10-05"}"""))).jsonObject.str("id")
        val weekly = demo.result(create(repeat("""{"freq":"weekly"}"""))).jsonObject.str("id")
        demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"$once"}""")
        demo.request("haac_bridge/schedules/run_now", """{"schedule_id":"$weekly"}""")
        val ids = demo.list(demo.ask("haac_bridge/schedules/list"), "schedules").map { it.str("id") }
        assertTrue(once !in ids)
        assertTrue(weekly in ids)
    }
}

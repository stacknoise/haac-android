package com.stacknoise.haac.feature.schedules.data

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/** The fields of the extension in the schedule replies (concept 19.10, 19.11) and the cache row built from them. */
class ScheduleWireTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val base = """
        "id":"a","owner":"u1","own":true,"name":"Heater","enabled":false,
        "when":{"type":"time","time":"06:30","days":[]},"action":"turn_on","entities":["switch.a"],
        "created_at":"2026-10-01T05:12:00+00:00","updated_at":"2026-10-02T05:12:00+00:00"
    """.trimIndent()

    private fun row(extra: String) =
        json.decodeFromString(ScheduleDescriptor.serializer(), "{$base$extra}").toRow("s1", "own", 5)

    @Test
    fun `a schedule of an older bridge is weekly, every week, without notifications`() {
        val row = row("")
        assertEquals("weekly", row.repeatFreq)
        assertEquals(1, row.repeatInterval)
        assertFalse(row.notify)
        assertFalse(row.expired)
        assertEquals("[]", row.blockedBy)
        assertEquals("[]", row.nextRuns)
    }

    @Test
    fun `the new fields reach the cache row`() {
        val row = row(
            ""","repeat":{"freq":"monthly","interval":2,"start_date":"2026-10-15","end_date":"2027-01-15"},
            "notify":true,"off_reason":"entity_disabled","blocked_by":["switch.a"],"expired":true,
            "next_runs":["2026-11-15T06:30:00+01:00"]""",
        )
        assertEquals(listOf("monthly", 2), listOf(row.repeatFreq, row.repeatInterval))
        assertEquals(listOf("2026-10-15", "2027-01-15"), listOf(row.startDate, row.endDate))
        assertEquals("entity_disabled", row.offReason)
        assertEquals("""["switch.a"]""", row.blockedBy)
        assertEquals("""["2026-11-15T06:30:00+01:00"]""", row.nextRuns)
    }

    @Test
    fun `a run log answer keeps the cursor and the entries`() {
        val runs = json.decodeFromString(
            ScheduleRuns.serializer(),
            """{"log_id":"l1","last_seq":7,"entries":[{"seq":7,"schedule_id":"a","name":"Heater",
            "at":"2026-10-26T06:30:00+01:00","result":"ok","removed":true,"notify":true,"future":1}]}""",
        )
        assertEquals(7L, runs.lastSeq)
        assertEquals(listOf("plan", true), listOf(runs.entries.single().trigger, runs.entries.single().removed))
    }

    @Test
    fun `a preview answer lists the runs`() {
        val preview = json.decodeFromString(SchedulePreview.serializer(), """{"next_runs":["a","b"]}""")
        assertEquals(2, preview.nextRuns.size)
    }
}

package com.stacknoise.haac.feature.entities.domain

import com.stacknoise.haac.core.database.entity.ExposedEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** [HistoryChartFactory]: which query per entity and how the answer becomes a chart (concept 8.1 – 8.4). */
class HistoryChartFactoryTest {
    private val factory = DefaultHistoryChartFactory()
    private val day = HistoryWindow(start = 0, end = 86_400_000, preset = HistoryPreset.DAY)
    private val week = day.copy(end = 7 * 86_400_000L, preset = HistoryPreset.WEEK)

    private val lamp = ExposedEntity("s1", "switch.lamp", "switch", "Lamp")
    private val temperature =
        ExposedEntity("s1", "sensor.t", "sensor", "T", unit = "°C", stateClass = "measurement")
    private val energy = ExposedEntity("s1", "sensor.e", "sensor", "E", unit = "kWh", stateClass = "total_increasing")
    private val radiator = ExposedEntity("s1", "climate.r", "climate", "R")

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun `the query follows domain, state class and range`() {
        assertEquals(HistoryQuery.States(attributes = false), factory.query(lamp, week))
        assertEquals(HistoryQuery.States(attributes = true), factory.query(radiator, week))
        assertEquals(HistoryQuery.States(attributes = false), factory.query(temperature, day))
        assertEquals(
            HistoryQuery.Statistics(StatisticPeriod.HOUR, listOf("mean", "min", "max")),
            factory.query(temperature, week),
        )
        assertEquals(HistoryQuery.Statistics(StatisticPeriod.HOUR, listOf("sum"), lead = 1), factory.query(energy, day))
        assertEquals(StatisticPeriod.DAY, (factory.query(energy, week) as HistoryQuery.Statistics).period)
    }

    @Test
    fun `switch states become a timeline with merged repeats`() {
        val result = json(
            """{"switch.lamp":[{"s":"off","lu":-10.0},{"s":"on","lu":3600.0},{"s":"on","lu":4000.0},""" +
                """{"s":"unavailable","lu":7200.0}]}""",
        )
        val chart = factory.create(lamp, day, HistoryQuery.States(false), result) as HistoryChart.Timeline
        assertEquals(
            listOf(
                Segment(0, 3_600_000, "off"),
                Segment(3_600_000, 7_200_000, "on"),
                Segment(7_200_000, 86_400_000, "unavailable"),
            ),
            chart.segments,
        )
    }

    @Test
    fun `numeric states become a line with gaps`() {
        val result = json(
            """{"sensor.t":[{"s":"20.5","lu":0.0},{"s":"21","lc":600.0,"lu":650.0},""" +
                """{"s":"unavailable","lu":1200.0},{"s":"19","lu":1800.0}]}""",
        )
        val chart = factory.create(temperature, day, HistoryQuery.States(false), result) as HistoryChart.Line
        val segments = chart.series.single().segments
        assertEquals(
            listOf(ChartPoint(0, 20.5), ChartPoint(600_000, 21.0), ChartPoint(1_200_000, 21.0)),
            segments[0],
        )
        assertEquals(listOf(ChartPoint(1_800_000, 19.0), ChartPoint(86_400_000, 19.0)), segments[1])
        assertEquals("°C", chart.unit)
    }

    @Test
    fun `climate history has current, target and heating phases`() {
        val result = json(
            """{"climate.r":[{"s":"heat","a":{"current_temperature":19,"temperature":21,"hvac_action":"heating"},""" +
                """"lu":0.0},{"s":"heat","a":{"current_temperature":21,"temperature":21,"hvac_action":"idle"},""" +
                """"lu":3600.0}]}""",
        )
        val chart = factory.create(radiator, day, HistoryQuery.States(true), result) as HistoryChart.Line
        assertEquals(listOf(SeriesKind.CURRENT, SeriesKind.TARGET), chart.series.map { it.kind })
        assertEquals(19.0, chart.series[0].valueAt(1_000))
        assertEquals(listOf(Phase(0, 3_600_000, PhaseKind.HEATING)), chart.phases)
    }

    @Test
    fun `counter statistics become bars of the growth per period`() {
        val result = json(
            """{"sensor.e":[{"start":-3600000,"end":0,"sum":10.0},{"start":0,"end":3600000,"sum":10.5},""" +
                """{"start":3600000,"end":7200000,"sum":12.0}]}""",
        )
        val query = HistoryQuery.Statistics(StatisticPeriod.HOUR, listOf("sum"), lead = 1)
        val chart = factory.create(energy, day, query, result) as HistoryChart.Bars
        assertEquals(listOf(Bar(0, 3_600_000, 0.5), Bar(3_600_000, 7_200_000, 1.5)), chart.bars)
    }

    @Test
    fun `measurement statistics become a mean line with a band`() {
        val result = json("""{"sensor.t":[{"start":0,"end":3600000,"mean":20.0,"min":19.0,"max":21.0}]}""")
        val query = HistoryQuery.Statistics(StatisticPeriod.HOUR, listOf("mean", "min", "max"))
        val chart = factory.create(temperature, week, query, result) as HistoryChart.Line
        assertEquals(SeriesKind.MEAN, chart.series.single().kind)
        assertEquals(listOf(RangePoint(0, 19.0, 21.0)), chart.band)
    }

    @Test
    fun `an empty answer is an empty chart`() {
        assertTrue(factory.create(lamp, day, HistoryQuery.States(false), json("{}")).isEmpty)
    }
}

package com.stacknoise.haac.feature.schedules.domain

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The next [count] runs of a schedule after [now]: the bridge's own [ScheduleItem.nextRun] first, then, for a
 * fixed time, the following matching weekdays. The bridge reports only the first run and the app does not know HA's
 * time zone, so the zone offset is taken from the first run (a change of daylight saving time within these days
 * is not considered). Sun events move every day, and a schedule that is off or paused has no next run.
 */
fun nextRuns(item: ScheduleItem, now: Long, count: Int = NextRunCount): List<Long> {
    val first = item.nextRun?.takeIf { it > now } ?: return emptyList()
    val time = item.time
    return if (item.whenType != WhenType.TIME || time == null || item.days.isEmpty()) {
        listOf(first)
    } else {
        fixedTimeRuns(item.days, time, first, count)
    }
}

/** [first] and the following runs on [days] at [time], in the zone offset [first] implies. */
private fun fixedTimeRuns(days: List<Int>, time: LocalTime, first: Long, count: Int): List<Long> {
    val offset = offsetOf(first, time)
    val start = Instant.ofEpochMilli(first).atOffset(offset).toLocalDate()
    val later = generateSequence(start.plusDays(1)) { it.plusDays(1) }
        .take(WeekDays.COUNT)
        .filter { (it.dayOfWeek.value - 1) in days }
        .map { it.atTime(time).toInstant(offset).toEpochMilli() }
    return (sequenceOf(first) + later).take(count).toList()
}

/** The zone offset in which [instant] is at [time] of day, found from the two clocks and kept within a day's range. */
private fun offsetOf(instant: Long, time: LocalTime): ZoneOffset {
    val utc = Instant.ofEpochMilli(instant).atOffset(ZoneOffset.UTC).toLocalTime().toSecondOfDay()
    var seconds = (time.toSecondOfDay() - utc) % SecondsPerDay
    if (seconds > MaxOffset) seconds -= SecondsPerDay
    if (seconds < -MaxOffset) seconds += SecondsPerDay
    return ZoneOffset.ofTotalSeconds(seconds)
}

private const val SecondsPerDay = 24 * 60 * 60
private const val MaxOffset = 14 * 60 * 60

/** How many upcoming runs the detail screen lists. */
const val NextRunCount = 3

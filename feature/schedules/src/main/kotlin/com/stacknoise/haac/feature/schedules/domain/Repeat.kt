package com.stacknoise.haac.feature.schedules.domain

/** How the weekdays of a schedule read in the summary (design 3b, concept 19.7). */
enum class Repeat {
    EVERY_DAY,
    WEEKDAYS,
    WEEKEND,
    NONE,
    CUSTOM,
}

private val WEEKDAY_SET = setOf(0, 1, 2, 3, 4)
private val WEEKEND_SET = setOf(5, 6)

/** Classifies [days] (0 = Monday to 6 = Sunday): all days, Monday to Friday, Saturday and Sunday, none or others. */
fun repeatOf(days: Collection<Int>): Repeat = when (days.toSet()) {
    emptySet<Int>() -> Repeat.NONE
    WEEKDAY_SET -> Repeat.WEEKDAYS
    WEEKEND_SET -> Repeat.WEEKEND
    else -> if (days.toSet().size == WeekDays.COUNT) Repeat.EVERY_DAY else Repeat.CUSTOM
}

package com.stacknoise.haac.feature.schedules.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * The schedule as the editor holds it while the user changes it (concept 19.2, M-12). The limits are the bridge's
 * (name 1 to 60 characters, 1 to 20 entities, offset of at most 180 minutes in steps of 5); the bridge checks them
 * again and answers `HAB-SCH-001` for an invalid schedule. [repeat] is the date rule (concept 19.10); [days] count
 * only for a weekly one. [notify] is the opt-in for run notifications (19.11).
 */
data class ScheduleDraft(
    val name: String = "",
    val whenType: WhenType = WhenType.TIME,
    val time: LocalTime = DEFAULT_TIME,
    val days: Set<Int> = WEEKDAYS,
    val offsetMin: Int = 0,
    val action: ScheduleAction = ScheduleAction.TURN_ON,
    val entityIds: List<String> = emptyList(),
    val repeat: RepeatRule = RepeatRule(),
    val notify: Boolean = false,
) {
    /** True if the name is 1 to [MAX_NAME] characters after trimming. */
    val nameValid: Boolean get() = name.trim().length in 1..MAX_NAME

    /**
     * The draft with date rule frequency [freq] (concept 19.10): see [RepeatRule.switchedTo]; a weekly rule without
     * weekdays gets Monday to Friday, so the editor never shows it empty.
     */
    fun withFreq(freq: RepeatFreq, today: LocalDate): ScheduleDraft = copy(
        repeat = repeat.switchedTo(freq, today),
        days = if (freq == RepeatFreq.WEEKLY && days.isEmpty()) WEEKDAYS else days,
    )

    /** True if the date rule is weekly, so the weekdays are part of it. */
    val weekly: Boolean get() = repeat.freq == RepeatFreq.WEEKLY

    /** True if a weekday is needed and chosen, or the date rule is not weekly and needs none. */
    val daysValid: Boolean get() = !weekly || days.isNotEmpty()

    /**
     * True if the draft can be saved: a valid name, a valid date rule with the weekdays it needs and, when the
     * entities are edited ([withEntities]), 1 to [MAX_ENTITIES] of them. The entities of a foreign schedule are
     * not edited or checked.
     */
    fun isValid(withEntities: Boolean): Boolean =
        nameValid && daysValid && repeat.valid && (!withEntities || entityIds.size in 1..MAX_ENTITIES)

    /**
     * Takes over the changes someone else made ([fresh], which was [original] before): every field the user did not
     * change follows [fresh], the user's own changes stay (concept 19.7, HAAC-SCH-004).
     */
    fun rebase(original: ScheduleDraft, fresh: ScheduleDraft): ScheduleDraft = ScheduleDraft(
        name = if (name == original.name) fresh.name else name,
        whenType = if (whenType == original.whenType) fresh.whenType else whenType,
        time = if (time == original.time) fresh.time else time,
        days = if (days == original.days) fresh.days else days,
        offsetMin = if (offsetMin == original.offsetMin) fresh.offsetMin else offsetMin,
        action = if (action == original.action) fresh.action else action,
        entityIds = if (entityIds == original.entityIds) fresh.entityIds else entityIds,
        repeat = if (repeat == original.repeat) fresh.repeat else repeat,
        notify = if (notify == original.notify) fresh.notify else notify,
    )

    /** True if the trigger (type, time, weekdays of a weekly rule or offset) differs from [other]'s. */
    fun triggerDiffers(other: ScheduleDraft): Boolean =
        whenType != other.whenType || (weekly && days != other.days) ||
            if (whenType == WhenType.TIME) time != other.time else offsetMin != other.offsetMin


    /** Defaults and limits. */
    companion object {
        /** Longest name the bridge accepts. */
        const val MAX_NAME = 60

        /** Most entities in one schedule. */
        const val MAX_ENTITIES = 20

        /** Largest offset of a sun event in minutes, before or after. */
        const val MAX_OFFSET = 180

        /** Step of the offset stepper in minutes. */
        const val OFFSET_STEP = 5

        /** Step of the minute stepper in minutes. */
        const val MINUTE_STEP = 1

        /** The time a new schedule starts with. */
        val DEFAULT_TIME: LocalTime = LocalTime.of(7, 0)

        /** Monday to Friday. */
        val WEEKDAYS: Set<Int> = setOf(0, 1, 2, 3, 4)

        /** Saturday and Sunday. */
        val WEEKEND: Set<Int> = setOf(5, 6)

        /** Every day of the week. */
        val EVERY_DAY: Set<Int> = (0 until WeekDays.COUNT).toSet()

        /** The draft of an existing schedule. */
        fun of(item: ScheduleItem): ScheduleDraft = ScheduleDraft(
            name = item.name,
            whenType = item.whenType,
            time = item.time ?: DEFAULT_TIME,
            days = item.days.toSet(),
            offsetMin = item.offsetMin,
            action = item.action,
            entityIds = item.entityIds,
            repeat = item.repeat,
            notify = item.notify,
        )
    }
}

/** The time of day one hour later or earlier ([hours] may be negative), wrapping around midnight. */
fun LocalTime.plusHoursWrapped(hours: Int): LocalTime = withHour(Math.floorMod(hour + hours, HoursPerDay))

/** The minute [minutes] later or earlier, wrapping within the hour so the hour stays. */
fun LocalTime.plusMinutesWrapped(minutes: Int): LocalTime =
    withMinute(Math.floorMod(minute + minutes, MinutesPerHour))

/** [offset] moved by [steps] steps of [ScheduleDraft.OFFSET_STEP] minutes, kept within the bridge's limit. */
fun stepOffset(offset: Int, steps: Int): Int =
    (offset + steps * ScheduleDraft.OFFSET_STEP).coerceIn(-ScheduleDraft.MAX_OFFSET, ScheduleDraft.MAX_OFFSET)

private const val HoursPerDay = 24
private const val MinutesPerHour = 60

package com.stacknoise.haac.core.database.layout

/**
 * Triggers of the layout tables (concept 6.1, 12): the floor of a room must belong to the room's home. Room
 * cannot declare triggers, so the migration and the database callback create them.
 */
object LayoutTriggers {
    /** Condition that is true when a new room row links a floor of another home. */
    private const val ForeignFloor = "NEW.floor_id IS NOT NULL AND " +
        "(SELECT home_id FROM floor WHERE id = NEW.floor_id) IS NOT NEW.home_id"

    /** The `CREATE TRIGGER` statements, for inserts and updates of `room`. */
    val statements = listOf(
        "CREATE TRIGGER IF NOT EXISTS `room_floor_home_insert` BEFORE INSERT ON `room` WHEN $ForeignFloor " +
            "BEGIN SELECT RAISE(ABORT, 'floor of another home'); END",
        "CREATE TRIGGER IF NOT EXISTS `room_floor_home_update` BEFORE UPDATE OF `home_id`, `floor_id` ON `room` " +
            "WHEN $ForeignFloor BEGIN SELECT RAISE(ABORT, 'floor of another home'); END",
    )
}

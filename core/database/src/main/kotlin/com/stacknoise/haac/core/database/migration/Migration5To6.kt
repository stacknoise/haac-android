package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/** Schema 5 → 6 (concept 7.2, 7.3, 12): the tables `room_entity` and `entity_alias`. */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration5To6 : Migration(5, 6) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `room_entity` (`room_id` TEXT NOT NULL, `entity_id` TEXT NOT NULL, " +
            "`sort_order` INTEGER NOT NULL, `tile_size` TEXT NOT NULL, `added_at` INTEGER NOT NULL, " +
            "PRIMARY KEY(`room_id`, `entity_id`), FOREIGN KEY(`room_id`) REFERENCES `room`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_room_entity_entity_id` ON `room_entity` (`entity_id`)",
        "CREATE TABLE IF NOT EXISTS `entity_alias` (`server_id` TEXT NOT NULL, `entity_id` TEXT NOT NULL, " +
            "`alias` TEXT NOT NULL, PRIMARY KEY(`server_id`, `entity_id`), FOREIGN KEY(`server_id`) " +
            "REFERENCES `server`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
    )

    /** Runs the migration with the driver API. */
    override fun migrate(connection: SQLiteConnection) {
        statements.forEach(connection::execSQL)
    }

    /** Runs the migration with the SupportSQLite API that Room uses without a driver. */
    override fun migrate(db: SupportSQLiteDatabase) {
        statements.forEach(db::execSQL)
    }
}

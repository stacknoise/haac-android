package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL
import com.stacknoise.haac.core.database.layout.LayoutTriggers

/** Schema 4 → 5 (concept 6, 12): the tables `home`, `floor` and `room` and the floor trigger of `room`. */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration4To5 : Migration(4, 5) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `home` (`id` TEXT NOT NULL, `server_id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
            "`icon` TEXT, `sort_order` INTEGER NOT NULL, `deleted_at` INTEGER, PRIMARY KEY(`id`), " +
            "FOREIGN KEY(`server_id`) REFERENCES `server`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_home_server_id` ON `home` (`server_id`)",
        "CREATE TABLE IF NOT EXISTS `floor` (`id` TEXT NOT NULL, `home_id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
            "`level` INTEGER NOT NULL, `icon` TEXT, `sort_order` INTEGER NOT NULL, `deleted_at` INTEGER, " +
            "PRIMARY KEY(`id`), FOREIGN KEY(`home_id`) REFERENCES `home`(`id`) ON UPDATE NO ACTION " +
            "ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_floor_home_id` ON `floor` (`home_id`)",
        "CREATE TABLE IF NOT EXISTS `room` (`id` TEXT NOT NULL, `home_id` TEXT NOT NULL, `floor_id` TEXT, " +
            "`name` TEXT NOT NULL, `icon` TEXT, `sort_order` INTEGER NOT NULL, `deleted_at` INTEGER, " +
            "PRIMARY KEY(`id`), FOREIGN KEY(`home_id`) REFERENCES `home`(`id`) ON UPDATE NO ACTION " +
            "ON DELETE CASCADE , FOREIGN KEY(`floor_id`) REFERENCES `floor`(`id`) ON UPDATE NO ACTION " +
            "ON DELETE SET NULL )",
        "CREATE INDEX IF NOT EXISTS `index_room_home_id` ON `room` (`home_id`)",
        "CREATE INDEX IF NOT EXISTS `index_room_floor_id` ON `room` (`floor_id`)",
    ) + LayoutTriggers.statements

    /** Runs the migration with the driver API. */
    override fun migrate(connection: SQLiteConnection) {
        statements.forEach(connection::execSQL)
    }

    /** Runs the migration with the SupportSQLite API that Room uses without a driver. */
    override fun migrate(db: SupportSQLiteDatabase) {
        statements.forEach(db::execSQL)
    }
}

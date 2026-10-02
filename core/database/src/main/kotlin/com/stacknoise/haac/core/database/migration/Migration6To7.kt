package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Schema 6 → 7 (concept 12, 19.7): the table `schedule` (it fills at the next schedule sync) and the column
 * `detail` of `notification`, which holds the pause reason of a *schedule paused* entry.
 */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration6To7 : Migration(6, 7) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `schedule` (`server_id` TEXT NOT NULL, `schedule_id` TEXT NOT NULL, " +
            "`owner` TEXT NOT NULL, `owner_name` TEXT, `own` INTEGER NOT NULL, `name` TEXT NOT NULL, " +
            "`enabled` INTEGER NOT NULL, `when_type` TEXT NOT NULL, `time` TEXT, `days` INTEGER NOT NULL, " +
            "`offset_min` INTEGER, `action` TEXT NOT NULL, `entity_ids` TEXT NOT NULL, " +
            "`created_at` INTEGER NOT NULL, `updated_at` TEXT NOT NULL, `paused_reason` TEXT, " +
            "`paused_at` INTEGER, `last_run_at` INTEGER, `last_run_result` TEXT, `last_run_code` TEXT, " +
            "`next_run` INTEGER, `synced_at` INTEGER NOT NULL, PRIMARY KEY(`server_id`, `schedule_id`), " +
            "FOREIGN KEY(`server_id`) REFERENCES `server`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "ALTER TABLE `notification` ADD COLUMN `detail` TEXT",
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

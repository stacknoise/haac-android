package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Schema 7 → 8 (concept 12, 7.5, 8.5, 19.10, 19.11): the deactivation columns of `exposed_entity`, the repeat rule,
 * `notify`, `off_reason`, `blocked_by`, `expired` and `next_runs` of `schedule`, the run log cursor of `server`
 * and the empty table `entity_guard`. The defaults give existing schedules the rule "weekly, every week" and no
 * notifications; the next sync fills the columns.
 */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration7To8 : Migration(7, 8) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "ALTER TABLE `exposed_entity` ADD COLUMN `disabled` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `exposed_entity` ADD COLUMN `disabled_at` INTEGER",
        "ALTER TABLE `schedule` ADD COLUMN `repeat_freq` TEXT NOT NULL DEFAULT 'weekly'",
        "ALTER TABLE `schedule` ADD COLUMN `repeat_interval` INTEGER NOT NULL DEFAULT 1",
        "ALTER TABLE `schedule` ADD COLUMN `start_date` TEXT",
        "ALTER TABLE `schedule` ADD COLUMN `end_date` TEXT",
        "ALTER TABLE `schedule` ADD COLUMN `notify` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `schedule` ADD COLUMN `off_reason` TEXT",
        "ALTER TABLE `schedule` ADD COLUMN `blocked_by` TEXT NOT NULL DEFAULT '[]'",
        "ALTER TABLE `schedule` ADD COLUMN `expired` INTEGER NOT NULL DEFAULT 0",
        "ALTER TABLE `schedule` ADD COLUMN `next_runs` TEXT NOT NULL DEFAULT '[]'",
        "ALTER TABLE `server` ADD COLUMN `schedule_run_log_id` TEXT",
        "ALTER TABLE `server` ADD COLUMN `schedule_run_seq` INTEGER",
        "CREATE TABLE IF NOT EXISTS `entity_guard` (`server_id` TEXT NOT NULL, `entity_id` TEXT NOT NULL, " +
            "`on_turn_on` INTEGER NOT NULL, `on_turn_off` INTEGER NOT NULL, PRIMARY KEY(`server_id`, `entity_id`), " +
            "FOREIGN KEY(`server_id`) REFERENCES `server`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
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

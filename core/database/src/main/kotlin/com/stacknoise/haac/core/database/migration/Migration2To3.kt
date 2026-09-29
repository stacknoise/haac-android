package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/** Schema 2 → 3 (concept 9.1, 12): the cache table `exposed_entity`; it fills at the next sync. */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration2To3 : Migration(2, 3) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `exposed_entity` (`server_id` TEXT NOT NULL, `entity_id` TEXT NOT NULL, " +
            "`domain` TEXT NOT NULL, `ha_name` TEXT NOT NULL, `configured_name` TEXT, `device_class` TEXT, " +
            "`unit` TEXT, `state_class` TEXT, `display_precision` INTEGER, `area` TEXT, " +
            "`supported_features` INTEGER NOT NULL, `status` TEXT NOT NULL, `withdrawn_at` INTEGER, " +
            "`last_state` TEXT, PRIMARY KEY(`server_id`, `entity_id`), FOREIGN KEY(`server_id`) " +
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

package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/** Schema 3 → 4 (concept 9.1, 12, 17.4): the table `notification` of the notification list. */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration3To4 : Migration(3, 4) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `notification` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`server_id` TEXT, `type` TEXT NOT NULL, `error_code` TEXT, `bridge_code` TEXT, " +
            "`count` INTEGER NOT NULL, `entity_ids` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
            "`read_at` INTEGER, `resolved_at` INTEGER, FOREIGN KEY(`server_id`) REFERENCES `server`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_notification_server_id` ON `notification` (`server_id`)",
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

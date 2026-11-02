package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Schema 8 → 9 (concept 12, 19.12): the column `upcoming_runs` of `schedule`, the JSON list of the next runs with
 * their date and skip flag. The default is an empty list; the next schedule sync fills it.
 */
@Suppress("MagicNumber") // The schema versions are the migration's identity.
object Migration8To9 : Migration(8, 9) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "ALTER TABLE `schedule` ADD COLUMN `upcoming_runs` TEXT NOT NULL DEFAULT '[]'",
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

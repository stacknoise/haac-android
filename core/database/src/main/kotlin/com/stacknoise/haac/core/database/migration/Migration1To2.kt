package com.stacknoise.haac.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

/**
 * Schema 1 → 2 (concept 4.5, 12): `base_url` and `pinned_key_hash` move into the internal slot for
 * `http://` addresses and into the external slot for `https://` addresses. `instance_uuid` stays empty
 * until the next connection. SQLite before 3.35 cannot drop columns, so the table is rebuilt.
 */
object Migration1To2 : Migration(1, 2) {
    /** Statements of the migration, shared by both database APIs. */
    val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `server_new` (`id` TEXT NOT NULL, `instance_uuid` TEXT, " +
            "`internal_url` TEXT, `external_url` TEXT, `internal_pinned_key_hash` TEXT, " +
            "`external_pinned_key_hash` TEXT, `always_use_internal` INTEGER NOT NULL DEFAULT 0, " +
            "`display_name` TEXT NOT NULL, `accent_color` INTEGER NOT NULL, `ha_user_name` TEXT NOT NULL, " +
            "`ha_version` TEXT NOT NULL, `bridge_api_version` INTEGER NOT NULL, `exposure_revision` TEXT, " +
            "`last_sync_at` INTEGER, `last_active_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "INSERT INTO `server_new` (`id`, `internal_url`, `external_url`, `internal_pinned_key_hash`, " +
            "`external_pinned_key_hash`, `display_name`, `accent_color`, `ha_user_name`, `ha_version`, " +
            "`bridge_api_version`, `exposure_revision`, `last_sync_at`, `last_active_at`) " +
            "SELECT `id`, " +
            "CASE WHEN `base_url` LIKE 'http://%' THEN `base_url` END, " +
            "CASE WHEN `base_url` LIKE 'http://%' THEN NULL ELSE `base_url` END, " +
            "CASE WHEN `base_url` LIKE 'http://%' THEN `pinned_key_hash` END, " +
            "CASE WHEN `base_url` LIKE 'http://%' THEN NULL ELSE `pinned_key_hash` END, " +
            "`display_name`, `accent_color`, `ha_user_name`, `ha_version`, `bridge_api_version`, " +
            "`exposure_revision`, `last_sync_at`, `last_active_at` FROM `server`",
        "DROP TABLE `server`",
        "ALTER TABLE `server_new` RENAME TO `server`",
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

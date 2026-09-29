package com.stacknoise.haac.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity

/**
 * The local database (concept 12). Tables of later chapters are added with versioned migrations
 * (package `migration`); destructive migration is never enabled.
 */
@Database(entities = [ServerEntity::class, ExposedEntity::class], version = 3, exportSchema = true)
abstract class HaacDatabase : RoomDatabase() {
    /** The `server` table. */
    abstract fun serverDao(): ServerDao

    /** The `exposed_entity` table (concept 9.1). */
    abstract fun exposedEntityDao(): ExposedEntityDao
}

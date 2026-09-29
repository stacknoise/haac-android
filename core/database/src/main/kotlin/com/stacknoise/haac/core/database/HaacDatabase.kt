package com.stacknoise.haac.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.stacknoise.haac.core.database.entity.ExposedEntity
import com.stacknoise.haac.core.database.entity.ExposedEntityDao
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.FloorEntity
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.HomeEntity
import com.stacknoise.haac.core.database.layout.LayoutTrashDao
import com.stacknoise.haac.core.database.layout.RoomDao
import com.stacknoise.haac.core.database.layout.RoomEntity
import com.stacknoise.haac.core.database.notification.NotificationDao
import com.stacknoise.haac.core.database.notification.NotificationEntity
import com.stacknoise.haac.core.database.server.ServerDao
import com.stacknoise.haac.core.database.server.ServerEntity

@Database(
    entities = [
        ServerEntity::class,
        ExposedEntity::class,
        NotificationEntity::class,
        HomeEntity::class,
        FloorEntity::class,
        RoomEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
/**
 * The local database (concept 12). Tables of later chapters are added with versioned migrations
 * (package `migration`); destructive migration is never enabled.
 */
abstract class HaacDatabase : RoomDatabase() {
    /** The `server` table. */
    abstract fun serverDao(): ServerDao

    /** The `exposed_entity` table (concept 9.1). */
    abstract fun exposedEntityDao(): ExposedEntityDao

    /** The `notification` table (concept 9.1, 17.4). */
    abstract fun notificationDao(): NotificationDao

    /** The `home` table (concept 6). */
    abstract fun homeDao(): HomeDao

    /** The `floor` table (concept 6). */
    abstract fun floorDao(): FloorDao

    /** The `room` table (concept 6). */
    abstract fun roomDao(): RoomDao

    /** Undo and purge of deleted homes, floors and rooms (concept 6.2). */
    abstract fun layoutTrashDao(): LayoutTrashDao
}

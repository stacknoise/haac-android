package com.stacknoise.haac.core.database.di

import com.stacknoise.haac.core.database.HaacDatabase
import com.stacknoise.haac.core.database.assignment.EntityAliasDao
import com.stacknoise.haac.core.database.assignment.RoomAssignmentDao
import com.stacknoise.haac.core.database.layout.FloorDao
import com.stacknoise.haac.core.database.layout.HomeDao
import com.stacknoise.haac.core.database.layout.LayoutTrashDao
import com.stacknoise.haac.core.database.layout.RoomDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** DAOs of the places and their entities (concept 6, 7). */
@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
    /** DAO of the `home` table. */
    @Provides
    fun provideHomeDao(database: HaacDatabase): HomeDao = database.homeDao()

    /** DAO of the `floor` table. */
    @Provides
    fun provideFloorDao(database: HaacDatabase): FloorDao = database.floorDao()

    /** DAO of the `room` table. */
    @Provides
    fun provideRoomDao(database: HaacDatabase): RoomDao = database.roomDao()

    /** DAO for undo and purge of deleted places. */
    @Provides
    fun provideLayoutTrashDao(database: HaacDatabase): LayoutTrashDao = database.layoutTrashDao()

    /** DAO of the `room_entity` table. */
    @Provides
    fun provideRoomAssignmentDao(database: HaacDatabase): RoomAssignmentDao = database.roomAssignmentDao()

    /** DAO of the `entity_alias` table. */
    @Provides
    fun provideEntityAliasDao(database: HaacDatabase): EntityAliasDao = database.entityAliasDao()
}

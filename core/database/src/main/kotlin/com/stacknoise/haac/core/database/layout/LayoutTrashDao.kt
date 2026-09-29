package com.stacknoise.haac.core.database.layout

import androidx.room.Dao
import androidx.room.Query

/**
 * Undo and purge of deleted homes, floors and rooms (concept 6.2). One deletion marks all its rows with the same
 * time, so that time identifies what an undo restores.
 */
@Dao
interface LayoutTrashDao {
    /** Restores the homes deleted at [at]. */
    @Query("UPDATE home SET deleted_at = NULL WHERE deleted_at = :at")
    suspend fun restoreHomes(at: Long)

    /** Restores the floors deleted at [at]. */
    @Query("UPDATE floor SET deleted_at = NULL WHERE deleted_at = :at")
    suspend fun restoreFloors(at: Long)

    /** Restores the rooms deleted at [at]. */
    @Query("UPDATE room SET deleted_at = NULL WHERE deleted_at = :at")
    suspend fun restoreRooms(at: Long)

    /** Deletes the rooms marked at or before [before]. */
    @Query("DELETE FROM room WHERE deleted_at <= :before")
    suspend fun purgeRooms(before: Long)

    /** Deletes the floors marked at or before [before]; their remaining rooms lose the floor. */
    @Query("DELETE FROM floor WHERE deleted_at <= :before")
    suspend fun purgeFloors(before: Long)

    /** Deletes the homes marked at or before [before] with everything in them. */
    @Query("DELETE FROM home WHERE deleted_at <= :before")
    suspend fun purgeHomes(before: Long)
}

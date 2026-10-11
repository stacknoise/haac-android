package com.stacknoise.haac.core.database.layout

import androidx.room.Dao
import androidx.room.Query

private const val OwnHomes = "home_id IN (SELECT id FROM home WHERE server_id = :serverId)"
private const val RestoreFloors = "UPDATE floor SET deleted_at = NULL WHERE deleted_at = :at AND $OwnHomes"
private const val RestoreRooms = "UPDATE room SET deleted_at = NULL WHERE deleted_at = :at AND $OwnHomes"

/**
 * Undo and purge of deleted homes, floors and rooms (concept 6.2). One deletion marks all its rows with the same
 * time; an undo restores the rows with that time of its own instance only, because two instances can mark rows in
 * the same millisecond (concept 12).
 */
@Dao
interface LayoutTrashDao {
    /** Restores the homes of instance [serverId] deleted at [at]. */
    @Query("UPDATE home SET deleted_at = NULL WHERE deleted_at = :at AND server_id = :serverId")
    suspend fun restoreHomes(at: Long, serverId: String)

    /** Restores the floors deleted at [at] in homes of instance [serverId]. */
    @Query(RestoreFloors)
    suspend fun restoreFloors(at: Long, serverId: String)

    /** Restores the rooms deleted at [at] in homes of instance [serverId]. */
    @Query(RestoreRooms)
    suspend fun restoreRooms(at: Long, serverId: String)

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

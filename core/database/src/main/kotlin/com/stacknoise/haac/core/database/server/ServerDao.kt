package com.stacknoise.haac.core.database.server

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Access to the `server` table (concept 12). */
@Dao
interface ServerDao {
    /** Adds a new instance. */
    @Insert
    suspend fun insert(server: ServerEntity)

    /** Replaces the row with the same id. */
    @Update
    suspend fun update(server: ServerEntity)

    /** The instance with [id], or null. */
    @Query("SELECT * FROM server WHERE id = :id")
    suspend fun get(id: String): ServerEntity?

    /** The instance of HA installation [uuid] signed in as HA user [user] (any case), or null (concept 4.5). */
    @Query("SELECT * FROM server WHERE instance_uuid = :uuid AND ha_user_name = :user COLLATE NOCASE LIMIT 1")
    suspend fun findByInstance(uuid: String, user: String): ServerEntity?

    /** The instance used last, or null if there is none. */
    @Query("SELECT * FROM server ORDER BY last_active_at DESC LIMIT 1")
    suspend fun mostRecent(): ServerEntity?

    /** The instance with [id] while it exists. */
    @Query("SELECT * FROM server WHERE id = :id")
    fun observe(id: String): Flow<ServerEntity?>

    /** All instances, sorted by name, then HA user (instance switcher, concept 4.4). */
    @Query("SELECT * FROM server ORDER BY display_name COLLATE NOCASE, ha_user_name COLLATE NOCASE")
    fun observeAll(): Flow<List<ServerEntity>>

    /** Marks instance [id] as used at [at] (epoch milliseconds). */
    @Query("UPDATE server SET last_active_at = :at WHERE id = :id")
    suspend fun touch(id: String, at: Long)

    /** Sets the local display name and accent colour of instance [id] (concept 4.4; never sent to HA). */
    @Query("UPDATE server SET display_name = :name, accent_color = :accent WHERE id = :id")
    suspend fun setAppearance(id: String, name: String, accent: Long)

    /** Deletes instance [id]; its cache, layout, assignments and aliases follow by cascade (concept 12). */
    @Query("DELETE FROM server WHERE id = :id")
    suspend fun delete(id: String)

    /** Number of instances; used to pick the accent colour of a new one. */
    @Query("SELECT COUNT(*) FROM server")
    suspend fun count(): Int
}

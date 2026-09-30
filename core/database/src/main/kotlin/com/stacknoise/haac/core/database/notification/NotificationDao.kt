package com.stacknoise.haac.core.database.notification

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Unread entries of an instance and global ones. */
private const val UnreadCountQuery = "SELECT COUNT(*) FROM notification " +
    "WHERE (server_id = :serverId OR server_id IS NULL) AND read_at IS NULL"

/** The newest error entry with a code since a time; `IS` also matches a null (global) instance. */
private const val RecentErrorQuery = "SELECT * FROM notification WHERE type = 'ERROR' AND error_code = :code " +
    "AND server_id IS :serverId AND created_at >= :since ORDER BY created_at DESC LIMIT 1"

/** Marks the unread entries of an instance and the global ones read. */
private const val MarkAllReadQuery = "UPDATE notification SET read_at = :at " +
    "WHERE (server_id = :serverId OR server_id IS NULL) AND read_at IS NULL"

/** Access to the `notification` table (concept 9.1, 12, 17.4); lists show an instance plus global entries. */
@Dao
interface NotificationDao {
    /** Adds an entry and returns its id. */
    @Insert
    suspend fun insert(entry: NotificationEntity): Long

    /** Entries of instance [serverId] and global ones, newest first. */
    @Query("SELECT * FROM notification WHERE server_id = :serverId OR server_id IS NULL ORDER BY created_at DESC")
    fun observe(serverId: String?): Flow<List<NotificationEntity>>

    /** Number of unread entries of instance [serverId] and global ones. */
    @Query(UnreadCountQuery)
    fun unreadCount(serverId: String?): Flow<Int>

    /** The newest error entry with [code] of [serverId] (null = global) created at or after [since], or null. */
    @Query(RecentErrorQuery)
    suspend fun recentError(serverId: String?, code: String, since: Long): NotificationEntity?

    /** Counts one more occurrence of error entry [id] and marks it unread again (concept 17.4). */
    @Query("UPDATE notification SET count = count + 1, read_at = NULL WHERE id = :id")
    suspend fun countAgain(id: Long)

    /** Marks entry [id] read at [at]. */
    @Query("UPDATE notification SET read_at = :at WHERE id = :id AND read_at IS NULL")
    suspend fun markRead(id: Long, at: Long)

    /** Marks every entry of [serverId] and every global entry read at [at]. */
    @Query(MarkAllReadQuery)
    suspend fun markAllRead(serverId: String?, at: Long)

    /** Dismisses entry [id] (*Dismiss*, *Keep*); it stays in the list without actions and counts as read. */
    @Query("UPDATE notification SET resolved_at = :at, read_at = COALESCE(read_at, :at) WHERE id = :id")
    suspend fun resolve(id: Long, at: Long)

    /** Deletes entry [id]. */
    @Query("DELETE FROM notification WHERE id = :id")
    suspend fun delete(id: Long)

    /** Deletes every entry of [serverId] and every global entry, the ones a list shows. */
    @Query("DELETE FROM notification WHERE server_id = :serverId OR server_id IS NULL")
    suspend fun deleteAll(serverId: String?)

    /** Deletes entries created before [before] (30 days, concept 9.1). */
    @Query("DELETE FROM notification WHERE created_at < :before")
    suspend fun purge(before: Long)
}

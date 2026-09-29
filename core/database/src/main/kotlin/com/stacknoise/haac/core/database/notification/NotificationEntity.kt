package com.stacknoise.haac.core.database.notification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stacknoise.haac.core.database.server.ServerEntity

/** Kind of a notification entry (concept 9.1, 17.4). */
enum class NotificationType {
    ADDED,
    REMOVED,
    ERROR,
}

@Entity(
    tableName = "notification",
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["server_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("server_id")],
)
/**
 * One entry of the notification list (table `notification`, concept 9.1, 12, 17.4): the entities one sync
 * added or withdrew ([entityIds] as a JSON list), or an error with its code; [serverId] is null for errors
 * without an instance. [count] counts the entities, or how often the same error occurred within 10 minutes.
 */
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "server_id") val serverId: String?,
    val type: NotificationType,
    @ColumnInfo(name = "error_code") val errorCode: String? = null,
    @ColumnInfo(name = "bridge_code") val bridgeCode: String? = null,
    val count: Int = 1,
    @ColumnInfo(name = "entity_ids") val entityIds: String = "[]",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "read_at") val readAt: Long? = null,
    @ColumnInfo(name = "resolved_at") val resolvedAt: Long? = null,
)

package com.stacknoise.haac.core.database.schedule

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.stacknoise.haac.core.database.server.ServerEntity

@Entity(
    tableName = "schedule",
    primaryKeys = ["server_id", "schedule_id"],
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["server_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
/**
 * Cached copy of one schedule of instance [serverId] (table `schedule`, concept 12, 19.7); filled by the schedule
 * sync and never edited offline. [days] is a bitmask with bit 0 = Monday. [updatedAt] stays the bridge's own text,
 * because an edit must send it back unchanged (19.4). [own] tells whether the user owns the schedule; foreign ones
 * exist only for admins. [entityIds] is a JSON list; times are epoch milliseconds.
 */
data class ScheduleEntity(
    @ColumnInfo(name = "server_id") val serverId: String,
    @ColumnInfo(name = "schedule_id") val scheduleId: String,
    val owner: String,
    @ColumnInfo(name = "owner_name") val ownerName: String? = null,
    val own: Boolean,
    val name: String,
    val enabled: Boolean,
    @ColumnInfo(name = "when_type") val whenType: String,
    val time: String? = null,
    val days: Int,
    @ColumnInfo(name = "offset_min") val offsetMin: Int? = null,
    val action: String,
    @ColumnInfo(name = "entity_ids") val entityIds: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "paused_reason") val pausedReason: String? = null,
    @ColumnInfo(name = "paused_at") val pausedAt: Long? = null,
    @ColumnInfo(name = "last_run_at") val lastRunAt: Long? = null,
    @ColumnInfo(name = "last_run_result") val lastRunResult: String? = null,
    @ColumnInfo(name = "last_run_code") val lastRunCode: String? = null,
    @ColumnInfo(name = "next_run") val nextRun: Long? = null,
    @ColumnInfo(name = "synced_at") val syncedAt: Long,
)

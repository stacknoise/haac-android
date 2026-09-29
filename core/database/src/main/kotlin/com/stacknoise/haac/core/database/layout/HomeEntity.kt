package com.stacknoise.haac.core.database.layout

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.stacknoise.haac.core.database.server.ServerEntity

@Entity(
    tableName = "home",
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
 * A home of instance [serverId] (table `home`, concept 6.1, 12). [deletedAt] is set while its deletion can
 * still be undone (6.2); such rows are hidden and purged afterwards.
 */
data class HomeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "server_id") val serverId: String,
    val name: String,
    val icon: String? = null,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

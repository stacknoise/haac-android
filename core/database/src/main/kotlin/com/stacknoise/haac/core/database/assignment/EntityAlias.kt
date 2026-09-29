package com.stacknoise.haac.core.database.assignment

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.stacknoise.haac.core.database.server.ServerEntity

@Entity(
    tableName = "entity_alias",
    primaryKeys = ["server_id", "entity_id"],
    foreignKeys = [
        ForeignKey(
            entity = ServerEntity::class,
            parentColumns = ["id"],
            childColumns = ["server_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
/** Local name of entity [entityId] of instance [serverId] (table `entity_alias`, concept 7.3); never sent to HA. */
data class EntityAlias(
    @ColumnInfo(name = "server_id") val serverId: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val alias: String,
)

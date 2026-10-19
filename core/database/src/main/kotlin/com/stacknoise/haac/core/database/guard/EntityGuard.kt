package com.stacknoise.haac.core.database.guard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.stacknoise.haac.core.database.server.ServerEntity

@Entity(
    tableName = "entity_guard",
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
/**
 * Switch guard of entity [entityId] of instance [serverId] (table `entity_guard`, concept 8.5, 12): a tap that turns
 * the switch on needs a confirmation if [onTurnOn], one that turns it off if [onTurnOff]. A row exists only while
 * the guard is on; it is local to the device and never sent to HA.
 */
data class EntityGuard(
    @ColumnInfo(name = "server_id") val serverId: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    @ColumnInfo(name = "on_turn_on") val onTurnOn: Boolean,
    @ColumnInfo(name = "on_turn_off") val onTurnOff: Boolean,
)

package com.stacknoise.haac.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.stacknoise.haac.core.database.server.ServerEntity

/** Whether the bridge still exposes an entity to the user (concept 7.4). */
enum class EntityStatus {
    ACTIVE,
    WITHDRAWN,
}

@Entity(
    tableName = "exposed_entity",
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
 * Cached bridge data of one exposed entity of instance [serverId] (table `exposed_entity`, concept 7.1, 9.1,
 * 12). [lastState] is the last known state as JSON; a withdrawn entity keeps it for its inactive tile (7.4).
 */
data class ExposedEntity(
    @ColumnInfo(name = "server_id") val serverId: String,
    @ColumnInfo(name = "entity_id") val entityId: String,
    val domain: String,
    @ColumnInfo(name = "ha_name") val haName: String,
    @ColumnInfo(name = "configured_name") val configuredName: String? = null,
    @ColumnInfo(name = "device_class") val deviceClass: String? = null,
    val unit: String? = null,
    @ColumnInfo(name = "state_class") val stateClass: String? = null,
    @ColumnInfo(name = "display_precision") val displayPrecision: Int? = null,
    val area: String? = null,
    @ColumnInfo(name = "supported_features") val supportedFeatures: Int = 0,
    val status: EntityStatus = EntityStatus.ACTIVE,
    @ColumnInfo(name = "withdrawn_at") val withdrawnAt: Long? = null,
    @ColumnInfo(name = "last_state") val lastState: String? = null,
)

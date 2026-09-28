package com.stacknoise.haac.core.database.server

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One HA instance (table `server`, concept 12); holds no credentials. */
@Entity(tableName = "server")
data class ServerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "base_url") val baseUrl: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "accent_color") val accentColor: Long,
    @ColumnInfo(name = "ha_user_name") val haUserName: String,
    @ColumnInfo(name = "ha_version") val haVersion: String,
    @ColumnInfo(name = "bridge_api_version") val bridgeApiVersion: Int,
    @ColumnInfo(name = "pinned_key_hash") val pinnedKeyHash: String? = null,
    @ColumnInfo(name = "exposure_revision") val exposureRevision: String? = null,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null,
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long,
)

package com.stacknoise.haac.core.database.server

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One HA instance and HA user (table `server`, concept 12); holds no credentials. At least one of
 * [internalUrl] and [externalUrl] is set (concept 4.5). [scheduleRunLogId] and [scheduleRunSeq] are the cursor in
 * the bridge's run log (concept 19.11); both are null until the first run sync.
 */
@Entity(tableName = "server")
data class ServerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "instance_uuid") val instanceUuid: String? = null,
    @ColumnInfo(name = "internal_url") val internalUrl: String? = null,
    @ColumnInfo(name = "external_url") val externalUrl: String? = null,
    @ColumnInfo(name = "internal_pinned_key_hash") val internalPinnedKeyHash: String? = null,
    @ColumnInfo(name = "external_pinned_key_hash") val externalPinnedKeyHash: String? = null,
    @ColumnInfo(name = "always_use_internal", defaultValue = "0") val alwaysUseInternal: Boolean = false,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "accent_color") val accentColor: Long,
    @ColumnInfo(name = "ha_user_name") val haUserName: String,
    @ColumnInfo(name = "ha_version") val haVersion: String,
    @ColumnInfo(name = "bridge_api_version") val bridgeApiVersion: Int,
    @ColumnInfo(name = "exposure_revision") val exposureRevision: String? = null,
    @ColumnInfo(name = "last_sync_at") val lastSyncAt: Long? = null,
    @ColumnInfo(name = "last_active_at") val lastActiveAt: Long,
    @ColumnInfo(name = "schedule_run_log_id") val scheduleRunLogId: String? = null,
    @ColumnInfo(name = "schedule_run_seq") val scheduleRunSeq: Long? = null,
)
